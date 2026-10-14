package com.restosuite.service;

import com.restosuite.common.ApiException;
import com.restosuite.common.Money;
import com.restosuite.domain.AppSetting;
import com.restosuite.domain.Branch;
import com.restosuite.domain.DiningArea;
import com.restosuite.domain.DiningTable;
import com.restosuite.domain.DocumentSequence;
import com.restosuite.domain.ExpenseCategory;
import com.restosuite.domain.Ingredient;
import com.restosuite.domain.KitchenStation;
import com.restosuite.domain.MenuCategory;
import com.restosuite.domain.Modifier;
import com.restosuite.domain.PaymentMethod;
import com.restosuite.domain.Product;
import com.restosuite.domain.ProductVariant;
import com.restosuite.domain.Recipe;
import com.restosuite.domain.RecipeLine;
import com.restosuite.domain.Restaurant;
import com.restosuite.domain.TableOccupancy;
import com.restosuite.domain.TableReservation;
import com.restosuite.domain.TaxRate;
import com.restosuite.domain.UnitOfMeasure;
import com.restosuite.domain.Warehouse;
import com.restosuite.repo.AppSettingRepository;
import com.restosuite.repo.BranchRepository;
import com.restosuite.repo.DiningAreaRepository;
import com.restosuite.repo.DiningTableRepository;
import com.restosuite.repo.DocumentSequenceRepository;
import com.restosuite.repo.ExpenseCategoryRepository;
import com.restosuite.repo.IngredientRepository;
import com.restosuite.repo.KitchenStationRepository;
import com.restosuite.repo.MenuCategoryRepository;
import com.restosuite.repo.ModifierRepository;
import com.restosuite.repo.PaymentMethodRepository;
import com.restosuite.repo.ProductRepository;
import com.restosuite.repo.ProductVariantRepository;
import com.restosuite.repo.RecipeLineRepository;
import com.restosuite.repo.RecipeRepository;
import com.restosuite.repo.RestaurantRepository;
import com.restosuite.repo.TableOccupancyRepository;
import com.restosuite.repo.TableReservationRepository;
import com.restosuite.repo.TaxRateRepository;
import com.restosuite.repo.UnitOfMeasureRepository;
import com.restosuite.repo.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class CatalogService {

    private final Support support;
    private final AuditService audit;
    private final RestaurantRepository restaurants;
    private final BranchRepository branches;
    private final TaxRateRepository taxes;
    private final PaymentMethodRepository paymentMethods;
    private final UnitOfMeasureRepository units;
    private final KitchenStationRepository stations;
    private final DiningAreaRepository areas;
    private final DiningTableRepository tables;
    private final TableReservationRepository reservations;
    private final TableOccupancyRepository occupancy;
    private final MenuCategoryRepository categories;
    private final ProductRepository products;
    private final ProductVariantRepository variants;
    private final ModifierRepository modifiers;
    private final RecipeRepository recipes;
    private final RecipeLineRepository recipeLines;
    private final IngredientRepository ingredients;
    private final ExpenseCategoryRepository expenseCategories;
    private final DocumentSequenceRepository sequences;
    private final AppSettingRepository settings;
    private final WarehouseRepository warehouses;

    public CatalogService(Support support, AuditService audit, RestaurantRepository restaurants, BranchRepository branches,
                          TaxRateRepository taxes, PaymentMethodRepository paymentMethods, UnitOfMeasureRepository units,
                          KitchenStationRepository stations, DiningAreaRepository areas, DiningTableRepository tables,
                          TableReservationRepository reservations, TableOccupancyRepository occupancy,
                          MenuCategoryRepository categories, ProductRepository products, ProductVariantRepository variants,
                          ModifierRepository modifiers, RecipeRepository recipes, RecipeLineRepository recipeLines,
                          IngredientRepository ingredients, ExpenseCategoryRepository expenseCategories,
                          DocumentSequenceRepository sequences, AppSettingRepository settings, WarehouseRepository warehouses) {
        this.support = support;
        this.audit = audit;
        this.restaurants = restaurants;
        this.branches = branches;
        this.taxes = taxes;
        this.paymentMethods = paymentMethods;
        this.units = units;
        this.stations = stations;
        this.areas = areas;
        this.tables = tables;
        this.reservations = reservations;
        this.occupancy = occupancy;
        this.categories = categories;
        this.products = products;
        this.variants = variants;
        this.modifiers = modifiers;
        this.recipes = recipes;
        this.recipeLines = recipeLines;
        this.ingredients = ingredients;
        this.expenseCategories = expenseCategories;
        this.sequences = sequences;
        this.settings = settings;
        this.warehouses = warehouses;
    }

    public Restaurant restaurant() {
        return support.restaurant();
    }

    @Transactional
    public Restaurant saveRestaurant(Restaurant input) {
        Restaurant current = support.restaurant();
        current.setName(Support.required(input.getName(), "El nombre", 160));
        current.setLegalName(Support.clean(input.getLegalName(), 200));
        current.setTaxId(Support.clean(input.getTaxId(), 40));
        current.setPhone(Support.clean(input.getPhone(), 40));
        current.setEmail(Support.clean(input.getEmail(), 160));
        current.setAddress(Support.clean(input.getAddress(), 300));
        current.setLogoUrl(Support.clean(input.getLogoUrl(), 500));
        current.setCountryCode(Support.required(input.getCountryCode(), "El país", 2).toUpperCase());
        current.setCurrencyCode(Support.required(input.getCurrencyCode(), "La moneda", 3).toUpperCase());
        current.setTimezone(Support.required(input.getTimezone(), "La zona horaria", 80));
        current.setLocale(Support.required(input.getLocale(), "El idioma", 20));
        try {
            java.time.ZoneId.of(current.getTimezone());
        } catch (Exception ex) {
            throw ApiException.badRequest("La zona horaria no es válida. Use un identificador como America/Santo_Domingo.");
        }
        audit.record("SETTINGS_RESTAURANT", "RESTAURANT", current.getId(), current.getName());
        return current;
    }

    public List<Branch> branches() {
        return branches.findByRestaurantIdOrderByNameAsc(support.user().getRestaurantId());
    }

    @Transactional
    public Branch saveBranch(Branch input) {
        Branch branch = input.getId() == null ? new Branch() : branches.findByIdAndRestaurantId(input.getId(), support.user().getRestaurantId())
                .orElseThrow(() -> ApiException.notFound("La sucursal"));
        branch.setRestaurantId(support.user().getRestaurantId());
        branch.setCode(Support.required(input.getCode(), "El código", 20).toUpperCase());
        branch.setName(Support.required(input.getName(), "El nombre", 160));
        branch.setAddress(Support.clean(input.getAddress(), 300));
        branch.setPhone(Support.clean(input.getPhone(), 40));
        branch.setActive(input.getId() == null || input.isActive());
        Branch saved = branches.save(branch);
        if (input.getId() == null) {
            Warehouse warehouse = new Warehouse();
            warehouse.setBranchId(saved.getId());
            warehouse.setCode("MAIN");
            warehouse.setName("Almacén principal");
            warehouse.setActive(true);
            warehouses.save(warehouse);
            for (String[] seq : new String[][]{{"ORDER", "PED-"}, {"RECEIPT", "REC-"}, {"PURCHASE", "OC-"}}) {
                DocumentSequence sequence = new DocumentSequence();
                sequence.setBranchId(saved.getId());
                sequence.setDocType(seq[0]);
                sequence.setPrefix(seq[1]);
                sequence.setNextNumber(1L);
                sequence.setPadding(6);
                sequences.save(sequence);
            }
        }
        audit.record("BRANCH_SAVE", "BRANCH", saved.getId(), saved.getCode());
        return saved;
    }

    public List<TaxRate> taxes() {
        return taxes.findByRestaurantIdOrderByNameAsc(support.user().getRestaurantId());
    }

    @Transactional
    public TaxRate saveTax(TaxRate input) {
        TaxRate tax = input.getId() == null ? new TaxRate() : taxes.findById(input.getId()).filter(row -> row.getRestaurantId().equals(support.restaurant().getId()))
                .orElseThrow(() -> ApiException.notFound("El impuesto"));
        tax.setRestaurantId(support.restaurant().getId());
        tax.setName(Support.required(input.getName(), "El nombre", 80));
        if (input.getRate() == null || input.getRate().signum() < 0) {
            throw ApiException.badRequest("La tasa no puede ser negativa. Use 0 para exento.");
        }
        tax.setRate(input.getRate());
        tax.setActive(input.getId() == null || input.isActive());
        return taxes.save(tax);
    }

    public List<PaymentMethod> paymentMethods() {
        return paymentMethods.findByRestaurantIdOrderBySortOrderAsc(support.user().getRestaurantId());
    }

    @Transactional
    public PaymentMethod savePaymentMethod(PaymentMethod input) {
        PaymentMethod method = input.getId() == null ? new PaymentMethod() : paymentMethods.findByIdAndRestaurantId(input.getId(), support.restaurant().getId())
                .orElseThrow(() -> ApiException.notFound("El método de pago"));
        method.setRestaurantId(support.restaurant().getId());
        method.setCode(Support.required(input.getCode(), "El código", 30).toUpperCase());
        method.setName(Support.required(input.getName(), "El nombre", 80));
        method.setAffectsCash(input.isAffectsCash());
        method.setRequiresReference(input.isRequiresReference());
        method.setSortOrder(input.getSortOrder() == null ? 0 : input.getSortOrder());
        method.setActive(input.getId() == null || input.isActive());
        return paymentMethods.save(method);
    }

    public List<UnitOfMeasure> units() {
        return units.findByRestaurantIdOrderByNameAsc(support.user().getRestaurantId());
    }

    @Transactional
    public UnitOfMeasure saveUnit(UnitOfMeasure input) {
        UnitOfMeasure unit = input.getId() == null ? new UnitOfMeasure() : units.findById(input.getId())
                .filter(row -> row.getRestaurantId().equals(support.restaurant().getId()))
                .orElseThrow(() -> ApiException.notFound("La unidad"));
        if (!List.of("COUNT", "WEIGHT", "VOLUME").contains(input.getFamily())) {
            throw ApiException.badRequest("La familia debe ser COUNT, WEIGHT o VOLUME.");
        }
        unit.setRestaurantId(support.restaurant().getId());
        unit.setCode(Support.required(input.getCode(), "El código", 20).toUpperCase());
        unit.setName(Support.required(input.getName(), "El nombre", 80));
        unit.setSymbol(Support.required(input.getSymbol(), "El símbolo", 12));
        unit.setFamily(input.getFamily());
        unit.setBaseUnitId(input.getBaseUnitId());
        unit.setFactorToBase(input.getFactorToBase() == null ? BigDecimal.ONE : input.getFactorToBase());
        unit.setActive(input.getId() == null || input.isActive());
        return units.save(unit);
    }

    public List<KitchenStation> stations(Long branchId) {
        support.access().checkBranch(branchId);
        return stations.findByBranchIdOrderBySortOrderAsc(branchId);
    }

    @Transactional
    public KitchenStation saveStation(KitchenStation input) {
        support.access().checkBranch(input.getBranchId());
        KitchenStation station = input.getId() == null ? new KitchenStation() : stations.findById(input.getId())
                .orElseThrow(() -> ApiException.notFound("La estación"));
        station.setBranchId(input.getBranchId());
        station.setCode(Support.required(input.getCode(), "El código", 20).toUpperCase());
        station.setName(Support.required(input.getName(), "El nombre", 80));
        station.setSortOrder(input.getSortOrder() == null ? 0 : input.getSortOrder());
        station.setActive(input.getId() == null || input.isActive());
        return stations.save(station);
    }

    public List<DiningArea> areas(Long branchId) {
        support.access().checkBranch(branchId);
        return areas.findByBranchIdOrderBySortOrderAsc(branchId);
    }

    @Transactional
    public DiningArea saveArea(DiningArea input) {
        support.access().checkBranch(input.getBranchId());
        DiningArea area = input.getId() == null ? new DiningArea() : areas.findById(input.getId()).orElseThrow(() -> ApiException.notFound("El área"));
        area.setBranchId(input.getBranchId());
        area.setCode(Support.required(input.getCode(), "El código", 20).toUpperCase());
        area.setName(Support.required(input.getName(), "El nombre", 80));
        area.setSortOrder(input.getSortOrder() == null ? 0 : input.getSortOrder());
        area.setActive(input.getId() == null || input.isActive());
        return areas.save(area);
    }

    public List<DiningTable> tables(Long branchId) {
        support.access().checkBranch(branchId);
        List<DiningTable> rows = new ArrayList<>();
        for (DiningArea area : areas(branchId)) {
            rows.addAll(tables.findByAreaIdOrderByTableNumberAsc(area.getId()));
        }
        return rows;
    }

    @Transactional
    public DiningTable saveTable(DiningTable input) {
        DiningArea area = areas.findById(input.getAreaId()).orElseThrow(() -> ApiException.notFound("El área"));
        support.access().checkBranch(area.getBranchId());
        DiningTable table = input.getId() == null ? new DiningTable() : tables.findById(input.getId()).orElseThrow(() -> ApiException.notFound("La mesa"));
        if (table.getId() == null) {
            table.setStatus("AVAILABLE");
        }
        table.setAreaId(area.getId());
        table.setTableNumber(Support.required(input.getTableNumber(), "El número", 20));
        if (input.getCapacity() == null || input.getCapacity() < 1) {
            throw ApiException.badRequest("La capacidad debe ser al menos 1.");
        }
        table.setCapacity(input.getCapacity());
        table.setNotes(Support.clean(input.getNotes(), 300));
        table.setActive(input.getId() == null || input.isActive());
        return tables.save(table);
    }

    @Transactional
    public DiningTable assignWaiter(Long tableId, Long waiterId) {
        DiningTable table = tables.findById(tableId).orElseThrow(() -> ApiException.notFound("La mesa"));
        DiningArea area = areas.findById(table.getAreaId()).orElseThrow();
        support.access().checkBranch(area.getBranchId());
        table.setAssignedWaiterId(waiterId);
        return table;
    }

    @Transactional
    public DiningTable release(Long tableId) {
        DiningTable table = tables.findById(tableId).orElseThrow(() -> ApiException.notFound("La mesa"));
        DiningArea area = areas.findById(table.getAreaId()).orElseThrow();
        support.access().checkBranch(area.getBranchId());
        if ("OCCUPIED".equals(table.getStatus())) {
            throw ApiException.conflict("La mesa tiene una cuenta abierta. Ciérrela antes de liberarla.");
        }
        table.setStatus("AVAILABLE");
        audit.record("TABLE_RELEASE", "TABLE", table.getId(), table.getTableNumber());
        return table;
    }

    @Transactional
    public TableReservation reserve(Long tableId, String customerName, String phone, Integer partySize, Instant from, Instant until, String notes) {
        DiningTable table = tables.findById(tableId).orElseThrow(() -> ApiException.notFound("La mesa"));
        DiningArea area = areas.findById(table.getAreaId()).orElseThrow();
        support.access().checkBranch(area.getBranchId());
        if (from == null || until == null || !until.isAfter(from)) {
            throw ApiException.badRequest("Indique un horario de reserva válido.");
        }
        TableReservation reservation = new TableReservation();
        reservation.setTableId(tableId);
        reservation.setCustomerName(Support.required(customerName, "El nombre", 160));
        reservation.setPhone(Support.clean(phone, 40));
        reservation.setPartySize(partySize == null || partySize < 1 ? 1 : partySize);
        reservation.setReservedFrom(from);
        reservation.setReservedUntil(until);
        reservation.setStatus("PENDING");
        reservation.setNotes(Support.clean(notes, 300));
        reservation.setCreatedBy(support.user().getId());
        if ("AVAILABLE".equals(table.getStatus())) {
            table.setStatus("RESERVED");
        }
        audit.record("TABLE_RESERVE", "TABLE", tableId, reservation.getCustomerName());
        return reservations.save(reservation);
    }

    public List<TableOccupancy> history(Long tableId) {
        DiningTable table = tables.findById(tableId).orElseThrow(() -> ApiException.notFound("La mesa"));
        DiningArea area = areas.findById(table.getAreaId()).orElseThrow();
        support.access().checkBranch(area.getBranchId());
        return occupancy.findByTableIdOrderByOpenedAtDesc(tableId);
    }

    public List<MenuCategory> categories() {
        return categories.findByRestaurantIdOrderBySortOrderAsc(support.user().getRestaurantId());
    }

    @Transactional
    public MenuCategory saveCategory(MenuCategory input) {
        MenuCategory category = input.getId() == null ? new MenuCategory() : categories.findById(input.getId())
                .filter(row -> row.getRestaurantId().equals(support.restaurant().getId()))
                .orElseThrow(() -> ApiException.notFound("La categoría"));
        category.setRestaurantId(support.restaurant().getId());
        category.setName(Support.required(input.getName(), "El nombre", 120));
        category.setSortOrder(input.getSortOrder() == null ? 0 : input.getSortOrder());
        category.setStationId(input.getStationId());
        category.setActive(input.getId() == null || input.isActive());
        return categories.save(category);
    }

    public List<Product> products() {
        return products.findByRestaurantIdAndActiveTrueOrderByNameAsc(support.user().getRestaurantId());
    }

    public List<Product> allProducts() {
        return products.findAll().stream().filter(product -> product.getRestaurantId().equals(support.user().getRestaurantId())).toList();
    }

    public ProductDetail product(Long id) {
        Product product = products.findByIdAndRestaurantId(id, support.user().getRestaurantId()).orElseThrow(() -> ApiException.notFound("El producto"));
        return new ProductDetail(product, variants.findByProductIdOrderBySortOrderAsc(id), products.modifierIds(id), recipeOf(id, null));
    }

    @Transactional
    public Product saveProduct(Product input, List<ProductVariant> variantRows, List<Long> modifierIds, List<RecipeLine> recipe) {
        Product product = input.getId() == null ? new Product() : products.findByIdAndRestaurantId(input.getId(), support.restaurant().getId())
                .orElseThrow(() -> ApiException.notFound("El producto"));
        product.setRestaurantId(support.restaurant().getId());
        product.setCategoryId(input.getCategoryId());
        product.setSku(Support.required(input.getSku(), "El SKU", 40).toUpperCase());
        product.setName(Support.required(input.getName(), "El nombre", 160));
        product.setDescription(Support.clean(input.getDescription(), 600));
        product.setImageUrl(Support.clean(input.getImageUrl(), 500));
        if (!List.of("FOOD", "DRINK").contains(input.getProductType())) {
            throw ApiException.badRequest("El tipo debe ser FOOD o DRINK.");
        }
        product.setProductType(input.getProductType());
        product.setBasePrice(Money.money(input.getBasePrice()));
        product.setTaxRateId(input.getTaxRateId());
        product.setPrepMinutes(input.getPrepMinutes() == null ? 10 : input.getPrepMinutes());
        product.setStationId(input.getStationId());
        product.setAvailable(input.isAvailable());
        product.setActive(input.getId() == null || input.isActive());
        product.setTrackInventory(input.isTrackInventory());
        Product saved = products.saveAndFlush(product);
        if (variantRows != null) {
            int order = 0;
            for (ProductVariant row : variantRows) {
                ProductVariant variant = row.getId() == null ? new ProductVariant() : variants.findById(row.getId()).orElseThrow();
                variant.setProductId(saved.getId());
                variant.setName(Support.required(row.getName(), "La variante", 80));
                variant.setPriceDelta(Money.money(row.getPriceDelta()));
                variant.setActive(row.isActive());
                variant.setSortOrder(order++);
                variants.save(variant);
            }
        }
        products.clearModifiers(saved.getId());
        if (modifierIds != null) {
            for (Long modifierId : modifierIds) {
                products.addModifier(saved.getId(), modifierId);
            }
        }
        replaceRecipe(saved.getId(), null, recipe);
        audit.record("PRODUCT_SAVE", "PRODUCT", saved.getId(), saved.getSku());
        return saved;
    }

    public List<Modifier> modifiers() {
        return modifiers.findByRestaurantIdOrderByNameAsc(support.user().getRestaurantId());
    }

    @Transactional
    public Modifier saveModifier(Modifier input) {
        Modifier modifier = input.getId() == null ? new Modifier() : modifiers.findById(input.getId())
                .filter(row -> row.getRestaurantId().equals(support.restaurant().getId()))
                .orElseThrow(() -> ApiException.notFound("El extra"));
        modifier.setRestaurantId(support.restaurant().getId());
        modifier.setName(Support.required(input.getName(), "El nombre", 80));
        modifier.setPrice(Money.money(input.getPrice()));
        modifier.setActive(input.getId() == null || input.isActive());
        return modifiers.save(modifier);
    }

    public List<Ingredient> ingredients() {
        return ingredients.findByRestaurantIdOrderByNameAsc(support.user().getRestaurantId());
    }

    @Transactional
    public Ingredient saveIngredient(Ingredient input) {
        Ingredient ingredient = input.getId() == null ? new Ingredient() : ingredients.findByIdAndRestaurantId(input.getId(), support.restaurant().getId())
                .orElseThrow(() -> ApiException.notFound("El ingrediente"));
        ingredient.setRestaurantId(support.restaurant().getId());
        ingredient.setSku(Support.required(input.getSku(), "El SKU", 40).toUpperCase());
        ingredient.setName(Support.required(input.getName(), "El nombre", 160));
        ingredient.setUnitId(input.getUnitId());
        ingredient.setMinStock(input.getMinStock() == null ? Money.qty(BigDecimal.ZERO) : Money.qty(input.getMinStock()));
        ingredient.setActive(input.getId() == null || input.isActive());
        return ingredients.save(ingredient);
    }

    @Transactional
    public void replaceRecipe(Long productId, Long variantId, List<RecipeLine> rows) {
        Product product = products.findByIdAndRestaurantId(productId, support.restaurant().getId()).orElseThrow(() -> ApiException.notFound("El producto"));
        Recipe recipe = variantId == null
                ? recipes.findByProductIdAndVariantIdIsNull(product.getId()).orElseGet(Recipe::new)
                : recipes.findByProductIdAndVariantId(product.getId(), variantId).orElseGet(Recipe::new);
        recipe.setProductId(product.getId());
        recipe.setVariantId(variantId);
        Recipe saved = recipes.saveAndFlush(recipe);
        recipeLines.deleteByRecipeId(saved.getId());
        if (rows == null) {
            return;
        }
        for (RecipeLine row : rows) {
            if (row.getQuantity() == null || row.getQuantity().signum() <= 0) {
                throw ApiException.badRequest("La cantidad de la receta debe ser mayor que cero.");
            }
            RecipeLine line = new RecipeLine();
            line.setRecipeId(saved.getId());
            line.setIngredientId(row.getIngredientId());
            line.setQuantity(Money.qty(row.getQuantity()));
            line.setUnitId(row.getUnitId());
            recipeLines.save(line);
        }
    }

    public List<RecipeLine> recipeOf(Long productId, Long variantId) {
        var recipe = variantId == null
                ? recipes.findByProductIdAndVariantIdIsNull(productId)
                : recipes.findByProductIdAndVariantId(productId, variantId);
        return recipe.map(value -> recipeLines.findByRecipeId(value.getId())).orElseGet(List::of);
    }

    public List<ProductVariant> variants(Long productId) {
        return variants.findByProductIdOrderBySortOrderAsc(productId);
    }

    public List<ExpenseCategory> expenseCategories() {
        return expenseCategories.findByRestaurantIdOrderByNameAsc(support.user().getRestaurantId());
    }

    @Transactional
    public ExpenseCategory saveExpenseCategory(ExpenseCategory input) {
        ExpenseCategory category = input.getId() == null ? new ExpenseCategory() : expenseCategories.findById(input.getId())
                .filter(row -> row.getRestaurantId().equals(support.restaurant().getId()))
                .orElseThrow(() -> ApiException.notFound("La categoría"));
        if (!List.of("OPERATING", "OTHER_EXPENSE", "OTHER_INCOME").contains(input.getClassification())) {
            throw ApiException.badRequest("La clasificación debe ser OPERATING, OTHER_EXPENSE u OTHER_INCOME.");
        }
        category.setRestaurantId(support.restaurant().getId());
        category.setName(Support.required(input.getName(), "El nombre", 120));
        category.setClassification(input.getClassification());
        category.setActive(input.getId() == null || input.isActive());
        return expenseCategories.save(category);
    }

    public List<AppSetting> parameters() {
        return settings.findByRestaurantIdOrderBySettingKeyAsc(support.restaurant().getId());
    }

    @Transactional
    public AppSetting saveParameter(String key, String value) {
        AppSetting setting = settings.findByRestaurantIdAndSettingKey(support.restaurant().getId(), key).orElseGet(AppSetting::new);
        setting.setRestaurantId(support.restaurant().getId());
        setting.setSettingKey(Support.required(key, "La clave", 80));
        setting.setSettingValue(Support.required(value, "El valor", 500));
        audit.record("SETTING", "SETTING", key, value);
        return settings.save(setting);
    }

    public List<DocumentSequence> sequences(Long branchId) {
        support.access().checkBranch(branchId);
        return sequences.findAll().stream().filter(row -> row.getBranchId().equals(branchId)).toList();
    }

    @Transactional
    public DocumentSequence saveSequence(DocumentSequence input) {
        support.access().checkBranch(input.getBranchId());
        DocumentSequence sequence = sequences.findByBranchIdAndDocType(input.getBranchId(), input.getDocType())
                .orElseGet(DocumentSequence::new);
        sequence.setBranchId(input.getBranchId());
        sequence.setDocType(Support.required(input.getDocType(), "El tipo", 30).toUpperCase());
        sequence.setPrefix(Support.required(input.getPrefix(), "El prefijo", 12));
        sequence.setNextNumber(input.getNextNumber() == null ? 1 : input.getNextNumber());
        sequence.setPadding(input.getPadding() == null ? 6 : input.getPadding());
        return sequences.save(sequence);
    }

    public List<Warehouse> warehouses(Long branchId) {
        support.access().checkBranch(branchId);
        return warehouses.findByBranchIdOrderByNameAsc(branchId);
    }

    public record ProductDetail(Product product, List<ProductVariant> variants, List<Long> modifierIds, List<RecipeLine> recipe) {
    }
}
