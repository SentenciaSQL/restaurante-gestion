package com.restosuite.service;

import com.restosuite.common.ApiException;
import com.restosuite.common.Codes;
import com.restosuite.common.Money;
import com.restosuite.domain.Ingredient;
import com.restosuite.domain.Product;
import com.restosuite.domain.Recipe;
import com.restosuite.domain.RecipeLine;
import com.restosuite.domain.SalesOrder;
import com.restosuite.domain.SalesOrderLine;
import com.restosuite.domain.StockLevel;
import com.restosuite.domain.StockMovement;
import com.restosuite.domain.UnitOfMeasure;
import com.restosuite.domain.Warehouse;
import com.restosuite.repo.IngredientRepository;
import com.restosuite.repo.ProductRepository;
import com.restosuite.repo.RecipeLineRepository;
import com.restosuite.repo.RecipeRepository;
import com.restosuite.repo.StockLevelRepository;
import com.restosuite.repo.StockMovementRepository;
import com.restosuite.repo.UnitOfMeasureRepository;
import com.restosuite.repo.WarehouseRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class InventoryService {

    private final StockLevelRepository levels;
    private final StockMovementRepository movements;
    private final WarehouseRepository warehouses;
    private final IngredientRepository ingredients;
    private final UnitOfMeasureRepository units;
    private final RecipeRepository recipes;
    private final RecipeLineRepository recipeLines;
    private final ProductRepository products;
    private final Support support;
    private final EntityManager em;

    public InventoryService(StockLevelRepository levels, StockMovementRepository movements, WarehouseRepository warehouses,
                            IngredientRepository ingredients, UnitOfMeasureRepository units, RecipeRepository recipes,
                            RecipeLineRepository recipeLines, ProductRepository products, Support support, EntityManager em) {
        this.levels = levels;
        this.movements = movements;
        this.warehouses = warehouses;
        this.ingredients = ingredients;
        this.units = units;
        this.recipes = recipes;
        this.recipeLines = recipeLines;
        this.products = products;
        this.support = support;
        this.em = em;
    }

    @Transactional(readOnly = true)
    public List<StockLevel> stock(Long branchId) {
        support.access().checkBranch(branchId);
        List<StockLevel> rows = new ArrayList<>();
        for (Warehouse warehouse : warehouses.findByBranchIdOrderByNameAsc(branchId)) {
            rows.addAll(levels.findByWarehouseId(warehouse.getId()));
        }
        return rows;
    }

    @Transactional(readOnly = true)
    public List<StockMovement> recentMovements(Long branchId) {
        support.access().checkBranch(branchId);
        List<StockMovement> rows = new ArrayList<>();
        for (Warehouse warehouse : warehouses.findByBranchIdOrderByNameAsc(branchId)) {
            rows.addAll(movements.findTop200ByWarehouseIdOrderByCreatedAtDesc(warehouse.getId()));
        }
        return rows;
    }

    public Warehouse mainWarehouse(Long branchId) {
        return warehouses.findByBranchIdAndCode(branchId, "MAIN")
                .or(() -> warehouses.findByBranchIdOrderByNameAsc(branchId).stream().filter(Warehouse::isActive).findFirst())
                .orElseThrow(() -> ApiException.badRequest("Cree un almacén activo en la sucursal antes de mover inventario."));
    }

    @Transactional
    public void deductOrderLines(SalesOrder order, List<SalesOrderLine> lines) {
        for (SalesOrderLine line : lines) {
            if (!line.isInventoryDeducted() && !Codes.CANCELLED.equals(line.getStatus())) {
                deductLine(order, line);
            }
        }
    }

    @Transactional
    public void deductLine(SalesOrder order, SalesOrderLine line) {
        if (line.isInventoryDeducted()) {
            return;
        }
        Product product = line.getProductId() == null ? null : products.findById(line.getProductId()).orElse(null);
        if (product == null || !product.isTrackInventory()) {
            line.setInventoryDeducted(true);
            line.setCostAmount(Money.cost(BigDecimal.ZERO));
            return;
        }
        Recipe recipe = null;
        if (line.getVariantId() != null) {
            recipe = recipes.findByProductIdAndVariantId(product.getId(), line.getVariantId()).orElse(null);
        }
        if (recipe == null) {
            recipe = recipes.findByProductIdAndVariantIdIsNull(product.getId()).orElse(null);
        }
        if (recipe == null) {
            line.setInventoryDeducted(true);
            line.setCostAmount(Money.cost(BigDecimal.ZERO));
            return;
        }
        Warehouse warehouse = mainWarehouse(order.getBranchId());
        BigDecimal total = BigDecimal.ZERO;
        for (RecipeLine recipeLine : recipeLines.findByRecipeId(recipe.getId())) {
            Ingredient ingredient = ingredients.findById(recipeLine.getIngredientId())
                    .orElseThrow(() -> ApiException.notFound("Un ingrediente de la receta"));
            BigDecimal needed = convert(recipeLine.getQuantity().multiply(line.getQuantity()), recipeLine.getUnitId(), ingredient.getUnitId());
            StockMovement movement = apply(warehouse.getId(), ingredient.getId(), Codes.OUT, needed, null, Codes.SALE,
                    "ORDER_LINE", line.getId().toString(), "Consumo por pedido " + order.getOrderNumber(),
                    support.user().getId(), "sale-" + line.getId() + "-" + ingredient.getId());
            total = total.add(movement.getUnitCost().multiply(needed));
        }
        line.setCostAmount(Money.cost(total));
        line.setInventoryDeducted(true);
    }

    @Transactional
    public void reverseLine(SalesOrderLine line, String reason) {
        if (!line.isInventoryDeducted() || line.isRestocked()) {
            return;
        }
        List<StockMovement> sales = movements.findByReferenceTypeAndReferenceId("ORDER_LINE", String.valueOf(line.getId()));
        for (StockMovement sale : sales) {
            if (!Codes.SALE.equals(sale.getMovementType()) || !Codes.OUT.equals(sale.getDirection())) {
                continue;
            }
            apply(sale.getWarehouseId(), sale.getIngredientId(), Codes.IN, sale.getQuantity(), sale.getUnitCost(),
                    Codes.SALE_REVERSAL, "ORDER_LINE", String.valueOf(line.getId()), reason, support.user().getId(),
                    "reversal-" + sale.getId());
        }
        line.setRestocked(true);
    }

    @Transactional
    public StockMovement move(Long branchId, Long warehouseId, Long ingredientId, String type, String direction,
                              BigDecimal quantity, BigDecimal unitCost, String reason) {
        support.access().checkBranch(branchId);
        if (reason == null || reason.isBlank()) {
            throw ApiException.badRequest("Indique el motivo del movimiento.");
        }
        Warehouse warehouse = warehouses.findById(warehouseId).orElseThrow(() -> ApiException.notFound("El almacén"));
        if (!warehouse.getBranchId().equals(branchId)) {
            throw ApiException.badRequest("El almacén no pertenece a la sucursal seleccionada.");
        }
        Ingredient ingredient = ingredients.findById(ingredientId).orElseThrow(() -> ApiException.notFound("El ingrediente"));
        if (!ingredient.getRestaurantId().equals(support.user().getRestaurantId())) {
            throw ApiException.forbidden("El ingrediente no pertenece a este restaurante.");
        }
        BigDecimal cost = unitCost;
        if (Codes.OUT.equals(direction) && cost == null) {
            cost = null;
        }
        return apply(warehouse.getId(), ingredient.getId(), direction, quantity, cost, type, "MANUAL", null,
                reason.trim(), support.user().getId(), null);
    }

    @Transactional
    public void transfer(Long branchId, Long sourceWarehouseId, Long targetWarehouseId, Long ingredientId,
                         BigDecimal quantity, String reason) {
        if (sourceWarehouseId.equals(targetWarehouseId)) {
            throw ApiException.badRequest("El almacén de origen y el de destino deben ser distintos.");
        }
        String group = UUID.randomUUID().toString();
        StockMovement out = move(branchId, sourceWarehouseId, ingredientId, Codes.TRANSFER_OUT, Codes.OUT, quantity, null, reason);
        out.setReferenceType("TRANSFER");
        out.setReferenceId(group);
        Warehouse target = warehouses.findById(targetWarehouseId).orElseThrow(() -> ApiException.notFound("El almacén destino"));
        if (!target.getBranchId().equals(branchId)) {
            throw ApiException.badRequest("Solo puede transferir dentro de la misma sucursal.");
        }
        apply(targetWarehouseId, ingredientId, Codes.IN, quantity, out.getUnitCost(), Codes.TRANSFER_IN,
                "TRANSFER", group, reason, support.user().getId(), "transfer-in-" + group);
    }

    public BigDecimal convert(BigDecimal quantity, Long fromUnitId, Long toUnitId) {
        if (fromUnitId.equals(toUnitId)) {
            return Money.qty(quantity);
        }
        UnitOfMeasure from = units.findById(fromUnitId).orElseThrow(() -> ApiException.notFound("La unidad de origen"));
        UnitOfMeasure to = units.findById(toUnitId).orElseThrow(() -> ApiException.notFound("La unidad de destino"));
        if (!from.getFamily().equals(to.getFamily())) {
            throw ApiException.badRequest("No se puede convertir " + from.getSymbol() + " a " + to.getSymbol()
                    + " porque no pertenecen a la misma familia de medida.");
        }
        BigDecimal inBase = quantity.multiply(from.getFactorToBase());
        return Money.qty(inBase.divide(to.getFactorToBase(), 8, Money.ROUND));
    }

    private StockMovement apply(Long warehouseId, Long ingredientId, String direction, BigDecimal quantity,
                                BigDecimal unitCost, String type, String referenceType, String referenceId,
                                String reason, Long userId, String idempotencyKey) {
        if (idempotencyKey != null) {
            var existing = movements.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        BigDecimal qty = Money.qty(quantity);
        if (qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw ApiException.badRequest("La cantidad del movimiento debe ser mayor que cero.");
        }
        lockPair(warehouseId, ingredientId);
        StockLevel level = levels.lock(warehouseId, ingredientId).orElseGet(() -> createLevel(warehouseId, ingredientId));
        BigDecimal cost;
        if (Codes.IN.equals(direction)) {
            cost = unitCost == null ? level.getAverageCost() : Money.cost(unitCost);
            BigDecimal oldQty = level.getQuantity();
            BigDecimal newQty = oldQty.add(qty);
            BigDecimal average;
            if (oldQty.compareTo(BigDecimal.ZERO) <= 0) {
                average = cost;
            } else {
                average = oldQty.multiply(level.getAverageCost()).add(qty.multiply(cost))
                        .divide(newQty, Money.COST_SCALE, Money.ROUND);
            }
            level.setQuantity(Money.qty(newQty));
            level.setAverageCost(Money.cost(average));
        } else if (Codes.OUT.equals(direction)) {
            boolean allowNegative = support.flag("inventory.allow_negative", false);
            if (!allowNegative && level.getQuantity().compareTo(qty) < 0) {
                Ingredient ingredient = ingredients.findById(ingredientId).orElseThrow();
                throw ApiException.conflict("Existencia insuficiente de " + ingredient.getName()
                        + ". Disponible: " + level.getQuantity().stripTrailingZeros().toPlainString()
                        + ". Registre una compra, un ajuste o habilite existencias negativas en parámetros.");
            }
            cost = level.getAverageCost();
            level.setQuantity(Money.qty(level.getQuantity().subtract(qty)));
        } else {
            throw ApiException.badRequest("La dirección del movimiento debe ser entrada o salida.");
        }
        level.setUpdatedAt(Instant.now());
        StockMovement movement = new StockMovement();
        movement.setWarehouseId(warehouseId);
        movement.setIngredientId(ingredientId);
        movement.setMovementType(type);
        movement.setDirection(direction);
        movement.setQuantity(qty);
        movement.setUnitCost(Money.cost(cost));
        movement.setReferenceType(referenceType);
        movement.setReferenceId(referenceId);
        movement.setReason(reason);
        movement.setUserId(userId);
        movement.setIdempotencyKey(idempotencyKey);
        return movements.save(movement);
    }

    private StockLevel createLevel(Long warehouseId, Long ingredientId) {
        StockLevel level = new StockLevel();
        level.setWarehouseId(warehouseId);
        level.setIngredientId(ingredientId);
        level.setQuantity(Money.qty(BigDecimal.ZERO));
        level.setAverageCost(Money.cost(BigDecimal.ZERO));
        level.setUpdatedAt(Instant.now());
        return levels.saveAndFlush(level);
    }

    private void lockPair(Long warehouseId, Long ingredientId) {
        em.createNativeQuery("select pg_advisory_xact_lock(cast(?1 as integer), cast(?2 as integer))")
                .setParameter(1, warehouseId)
                .setParameter(2, ingredientId)
                .getSingleResult();
    }
}
