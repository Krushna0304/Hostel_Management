package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.dto.InstallmentDistribution;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Service for calculating flexible installment distributions
 */
@Service
public class InstallmentDistributionService {

    /**
     * Calculate intelligent distribution of months across installments
     * Examples:
     * - 12 months, 3 installments = 4+4+4
     * - 10 months, 3 installments = 4+3+3
     * - 11 months, 4 installments = 3+3+3+2
     */
    public InstallmentDistribution calculateDistribution(int totalMonths, int numberOfInstallments) {
        if (totalMonths <= 0 || numberOfInstallments <= 0) {
            throw new IllegalArgumentException("Total months and number of installments must be positive");
        }
        
        if (numberOfInstallments > totalMonths) {
            throw new IllegalArgumentException("Number of installments cannot exceed total months");
        }

        List<InstallmentDistribution.InstallmentGroup> groups = new ArrayList<>();
        
        // Calculate base months per installment and remainder
        int baseMonthsPerInstallment = totalMonths / numberOfInstallments;
        int remainderMonths = totalMonths % numberOfInstallments;
        
        int currentMonth = 1;
        
        for (int i = 1; i <= numberOfInstallments; i++) {
            // First 'remainderMonths' installments get an extra month
            int monthsInThisInstallment = baseMonthsPerInstallment + (i <= remainderMonths ? 1 : 0);
            
            List<Integer> monthNumbers = new ArrayList<>();
            for (int j = 0; j < monthsInThisInstallment; j++) {
                monthNumbers.add(currentMonth++);
            }
            
            String description = formatMonthRange(monthNumbers);
            
            InstallmentDistribution.InstallmentGroup group = InstallmentDistribution.InstallmentGroup.builder()
                    .installmentNumber(i)
                    .monthNumbers(monthNumbers)
                    .monthCount(monthsInThisInstallment)
                    .description(description)
                    .build();
            
            groups.add(group);
        }
        
        return InstallmentDistribution.builder()
                .totalMonths(totalMonths)
                .numberOfInstallments(numberOfInstallments)
                .installmentGroups(groups)
                .distributionStrategy("AUTO")
                .build();
    }
    
    /**
     * Calculate equal distribution (may leave some months unassigned if not evenly divisible)
     */
    public InstallmentDistribution calculateEqualDistribution(int totalMonths, int numberOfInstallments) {
        if (totalMonths % numberOfInstallments != 0) {
            throw new IllegalArgumentException("Total months must be evenly divisible by number of installments for equal distribution");
        }
        
        List<InstallmentDistribution.InstallmentGroup> groups = new ArrayList<>();
        int monthsPerInstallment = totalMonths / numberOfInstallments;
        int currentMonth = 1;
        
        for (int i = 1; i <= numberOfInstallments; i++) {
            List<Integer> monthNumbers = IntStream.range(currentMonth, currentMonth + monthsPerInstallment)
                    .boxed()
                    .toList();
            
            String description = formatMonthRange(monthNumbers);
            
            InstallmentDistribution.InstallmentGroup group = InstallmentDistribution.InstallmentGroup.builder()
                    .installmentNumber(i)
                    .monthNumbers(monthNumbers)
                    .monthCount(monthsPerInstallment)
                    .description(description)
                    .build();
            
            groups.add(group);
            currentMonth += monthsPerInstallment;
        }
        
        return InstallmentDistribution.builder()
                .totalMonths(totalMonths)
                .numberOfInstallments(numberOfInstallments)
                .installmentGroups(groups)
                .distributionStrategy("EQUAL")
                .build();
    }
    
    private String formatMonthRange(List<Integer> monthNumbers) {
        if (monthNumbers.isEmpty()) {
            return "";
        }
        
        if (monthNumbers.size() == 1) {
            return "Month " + monthNumbers.get(0);
        }
        
        int first = monthNumbers.get(0);
        int last = monthNumbers.get(monthNumbers.size() - 1);
        
        return "Months " + first + "-" + last;
    }
}