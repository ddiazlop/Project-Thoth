package com.miajon.jobsearch.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.miajon.jobsearch.exception.PredictionException;
import com.miajon.jobsearch.model.Expense;
import com.miajon.jobsearch.model.forms.ExpenseForm;
import com.miajon.jobsearch.records.ExpenseRecords;
import com.miajon.jobsearch.records.PredictionRecords;
import com.miajon.jobsearch.records.ExpenseRecords.ExpensesAndIncomeByMonth;
import com.miajon.jobsearch.records.ExpenseRecords.ExpensesByType;
import com.miajon.jobsearch.records.ViewRecords.DashboardViewData;
import com.miajon.jobsearch.service.ExpenseService;
import com.miajon.jobsearch.tools.PredictionTools;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@CrossOrigin(origins = { "http://127.0.0.1:3000", "http://localhost:3000" })
public class ExpensesRestController {

    private final ExpenseService expenseService;

    public ExpensesRestController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @GetMapping("/api/expenses/overview")
    public ExpenseRecords.ExpenseOverview expenses()
            throws InterruptedException, JsonProcessingException, PredictionException {
        Double totalAmount = expenseService.getTotalAmount();
        Double totalMonthlyAmount = expenseService.getTotalMonthlyAmount();

        List<ExpenseRecords.ExpenseByMonth> expensesPerMonthList = expenseService.getIncomePerMonth();

        PredictionRecords.NumericPrediction prediction = null;
        try {
            prediction = PredictionTools.predictNextMonthExpense(expensesPerMonthList);
        } catch (PredictionException e) {
            prediction = new PredictionRecords.NumericPrediction(0.0, 0.0);
        }

        return new ExpenseRecords.ExpenseOverview(totalAmount, totalMonthlyAmount, prediction.prediction());
    }

    @GetMapping("/api/expenses/latest")
    public Iterable<Expense> latestExpenses() {
        return expenseService.findLastTenExpenses();
    }

    @GetMapping("/api/expenses/monthly")
    public Iterable<Expense> monthlyExpenses() {
        return expenseService.findMonthlyExpenses();
    }

    @GetMapping("/api/expenses/per-month")
    public String expensesPerMonth() {
        ExpensesAndIncomeByMonth expensesAndIncomeByMonth = ExpensesAndIncomeByMonth.ofOrdered(
                expenseService.getExpensesPerMonth(),
                expenseService.getIncomePerMonth());

        return expensesAndIncomeByMonth.toJson();
    }

    @GetMapping("/api/expenses/dashboard")
    public String getDashboardData() {
        ExpensesAndIncomeByMonth expensesAndIncomeByMonth = ExpensesAndIncomeByMonth.ofOrdered(
                expenseService.getExpensesPerMonth(),
                expenseService.getIncomePerMonth());

        // TODO: Change name of ExpensesByType to singular form
        List<ExpensesByType> expensesByType = expenseService.getAmountsByType();

        String dashBoardViewData = DashboardViewData.of(expensesAndIncomeByMonth,
                expensesByType).toJson();
        return dashBoardViewData;
    }

    @PostMapping("/api/expenses")
    public void addExpense(@ModelAttribute @Valid ExpenseForm expense) {
        Double realAmount = expense.isIncome() ? expense.getAmount() : -expense.getAmount();
        Expense expenseModel = new Expense(expense.getConcept(), realAmount, expense.getMonthly());
        expenseService.saveExpense(expenseModel);
    }

    @DeleteMapping(value = "/api/expenses/{id}")
    public void deleteExpense(@PathVariable String id) {
        expenseService.deleteExpense(id);

    }

}
