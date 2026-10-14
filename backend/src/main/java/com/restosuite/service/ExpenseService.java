package com.restosuite.service;

import com.restosuite.common.ApiException;
import com.restosuite.common.Codes;
import com.restosuite.common.Money;
import com.restosuite.domain.Expense;
import com.restosuite.domain.ExpenseCategory;
import com.restosuite.repo.ExpenseCategoryRepository;
import com.restosuite.repo.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
public class ExpenseService {

    private final ExpenseRepository expenses;
    private final ExpenseCategoryRepository categories;
    private final Support support;
    private final CashService cash;
    private final AuditService audit;

    public ExpenseService(ExpenseRepository expenses, ExpenseCategoryRepository categories, Support support, CashService cash, AuditService audit) {
        this.expenses = expenses;
        this.categories = categories;
        this.support = support;
        this.cash = cash;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<Expense> list(Long branchId, String status, LocalDate from, LocalDate to) {
        Long resolved = support.access().resolveBranch(branchId);
        return expenses.findAll().stream()
                .filter(expense -> resolved == null || expense.getBranchId().equals(resolved))
                .filter(expense -> status == null || status.isBlank() || expense.getStatus().equals(status))
                .filter(expense -> from == null || !expense.getExpenseDate().isBefore(from))
                .filter(expense -> to == null || !expense.getExpenseDate().isAfter(to))
                .toList();
    }

    @Transactional
    public Expense create(Expense input, boolean payNow) {
        support.access().checkBranch(input.getBranchId());
        ExpenseCategory category = categories.findById(input.getCategoryId()).orElseThrow(() -> ApiException.notFound("La categoría"));
        if (!category.getRestaurantId().equals(support.user().getRestaurantId())) {
            throw ApiException.notFound("La categoría");
        }
        Expense expense = new Expense();
        expense.setBranchId(input.getBranchId());
        expense.setCategoryId(category.getId());
        expense.setSupplierId(input.getSupplierId());
        expense.setDescription(Support.required(input.getDescription(), "La descripción", 300));
        expense.setExpenseDate(input.getExpenseDate() == null ? LocalDate.now(support.zone()) : input.getExpenseDate());
        expense.setAmount(Money.money(input.getAmount()));
        if (expense.getAmount().signum() <= 0) {
            throw ApiException.badRequest("El importe debe ser mayor que cero.");
        }
        expense.setCurrencyCode(support.restaurant().getCurrencyCode());
        expense.setPaymentMethodId(input.getPaymentMethodId());
        expense.setReceiptRef(Support.clean(input.getReceiptRef(), 80));
        expense.setNotes(Support.clean(input.getNotes(), 400));
        expense.setCreatedBy(support.user().getId());
        expense.setStatus(Codes.PENDING);
        expenses.saveAndFlush(expense);
        audit.record("EXPENSE_CREATE", "EXPENSE", expense.getId(), expense.getDescription());
        if (payNow) {
            return pay(expense.getId(), input.getPaymentMethodId());
        }
        return expense;
    }

    @Transactional
    public Expense pay(Long id, Long methodId) {
        Expense expense = expenses.findById(id).orElseThrow(() -> ApiException.notFound("El gasto"));
        support.access().checkBranch(expense.getBranchId());
        if (!Codes.PENDING.equals(expense.getStatus())) {
            throw ApiException.conflict("Solo un gasto pendiente se puede pagar. Si ya está pagado, no lo registre otra vez.");
        }
        var method = cash.method(methodId == null ? expense.getPaymentMethodId() : methodId);
        var session = method.isAffectsCash() ? cash.requireOpen(expense.getBranchId()) : cash.requireOpen(expense.getBranchId());
        var movement = cash.record(session, Codes.EXPENSE, Codes.OUT, expense.getAmount(), method.isAffectsCash(), method.getId(),
                "EXPENSE", expense.getId().toString(), expense.getDescription(), "expense-" + expense.getId());
        expense.setStatus(Codes.PAID);
        expense.setPaymentMethodId(method.getId());
        expense.setPaidAt(Instant.now());
        expense.setPaidBy(support.user().getId());
        if (movement != null) {
            expense.setCashMovementId(movement.getId());
        }
        audit.record("EXPENSE_PAY", "EXPENSE", expense.getId(), expense.getAmount().toPlainString());
        return expense;
    }

    @Transactional
    public Expense voidExpense(Long id, String reason) {
        Expense expense = expenses.findById(id).orElseThrow(() -> ApiException.notFound("El gasto"));
        support.access().checkBranch(expense.getBranchId());
        String why = Support.required(reason, "El motivo", 300);
        if (Codes.VOID.equals(expense.getStatus())) {
            throw ApiException.conflict("El gasto ya está anulado.");
        }
        if (Codes.PAID.equals(expense.getStatus()) && expense.getCashMovementId() != null) {
            var method = cash.methodExisting(expense.getPaymentMethodId());
            var session = cash.requireOpen(expense.getBranchId());
            cash.record(session, Codes.ADJUSTMENT, Codes.IN, expense.getAmount(), method.isAffectsCash(), method.getId(),
                    "EXPENSE", expense.getId().toString(), "Anulación: " + why, "expense-void-" + expense.getId());
        }
        expense.setStatus(Codes.VOID);
        expense.setVoidReason(why);
        audit.record("EXPENSE_VOID", "EXPENSE", expense.getId(), why);
        return expense;
    }
}
