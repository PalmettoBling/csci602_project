package edu.citadel.hw1;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HW1Test {

    private HourlyEmployee johnDoe;
    private HourlyEmployee janeDoe;
    private SalariedEmployee moeHoward;
    private SalariedEmployee curlyHoward;

    @BeforeEach
    void setUp() {
        johnDoe = new HourlyEmployee("John Doe", LocalDate.of(2009, 5, 21), 50.5, 160.0);
        janeDoe = new HourlyEmployee("Jane Doe", LocalDate.of(2005, 9, 1), 150.5, 80.0);
        moeHoward = new SalariedEmployee("Moe Howard", LocalDate.of(2004, 1, 1), 75000.0);
        curlyHoward = new SalariedEmployee("Curly Howard", LocalDate.of(2018, 1, 1), 105000.0);
    }

    @Nested
    @DisplayName("Employee class structure")
    class EmployeeStructure {

        @Test
        @DisplayName("Employee is abstract")
        void employeeIsAbstract() {
            assertTrue(Modifier.isAbstract(Employee.class.getModifiers()));
        }

        @Test
        @DisplayName("Employee implements Comparable<Employee>")
        void employeeImplementsComparable() {
            assertTrue(Comparable.class.isAssignableFrom(Employee.class));
        }

        @Test
        @DisplayName("HourlyEmployee extends Employee")
        void hourlyExtendsEmployee() {
            assertTrue(Employee.class.isAssignableFrom(HourlyEmployee.class));
        }

        @Test
        @DisplayName("SalariedEmployee extends Employee")
        void salariedExtendsEmployee() {
            assertTrue(Employee.class.isAssignableFrom(SalariedEmployee.class));
        }
    }

    @Nested
    @DisplayName("HourlyEmployee")
    class HourlyEmployeeTests {

        @Test
        @DisplayName("getName() returns correct name")
        void getName() {
            assertEquals("John Doe", johnDoe.getName());
        }

        @Test
        @DisplayName("getHireDate() returns correct date")
        void getHireDate() {
            assertEquals(LocalDate.of(2009, 5, 21), johnDoe.getHireDate());
        }

        @Test
        @DisplayName("getWageRate() returns correct wage rate")
        void getWageRate() {
            assertEquals(50.5, johnDoe.getWageRate(), 0.001);
        }

        @Test
        @DisplayName("getHoursWorked() returns correct hours")
        void getHoursWorked() {
            assertEquals(160.0, johnDoe.getHoursWorked(), 0.001);
        }

        @Test
        @DisplayName("getMonthlyPay() = wageRate * hoursWorked")
        void getMonthlyPay() {
            assertEquals(8080.0, johnDoe.getMonthlyPay(), 0.001);
            assertEquals(12040.0, janeDoe.getMonthlyPay(), 0.001);
        }

        @Test
        @DisplayName("toString() matches expected format")
        void toStringFormat() {
            assertEquals(
                "HourlyEmployee[name=John Doe, hireDate=2009-05-21, wageRate=50.5, hoursWorked=160.0]",
                johnDoe.toString()
            );
        }

        @Test
        @DisplayName("equals() for identical fields")
        void equalsIdentical() {
            HourlyEmployee copy = new HourlyEmployee("John Doe", LocalDate.of(2009, 5, 21), 50.5, 160.0);
            assertEquals(johnDoe, copy);
        }

        @Test
        @DisplayName("equals() for different fields")
        void equalsDifferent() {
            assertNotEquals(johnDoe, janeDoe);
        }

        @Test
        @DisplayName("hashCode() consistent with equals()")
        void hashCodeConsistent() {
            HourlyEmployee copy = new HourlyEmployee("John Doe", LocalDate.of(2009, 5, 21), 50.5, 160.0);
            assertEquals(johnDoe.hashCode(), copy.hashCode());
        }
    }

    @Nested
    @DisplayName("SalariedEmployee")
    class SalariedEmployeeTests {

        @Test
        @DisplayName("getName() returns correct name")
        void getName() {
            assertEquals("Moe Howard", moeHoward.getName());
        }

        @Test
        @DisplayName("getHireDate() returns correct date")
        void getHireDate() {
            assertEquals(LocalDate.of(2004, 1, 1), moeHoward.getHireDate());
        }

        @Test
        @DisplayName("getAnnualSalary() returns correct salary")
        void getAnnualSalary() {
            assertEquals(75000.0, moeHoward.getAnnualSalary(), 0.001);
        }

        @Test
        @DisplayName("getMonthlyPay() = annualSalary / 12")
        void getMonthlyPay() {
            assertEquals(6250.0, moeHoward.getMonthlyPay(), 0.001);
            assertEquals(8750.0, curlyHoward.getMonthlyPay(), 0.001);
        }

        @Test
        @DisplayName("toString() matches expected format")
        void toStringFormat() {
            assertEquals(
                "SalariedEmployee[name=Curly Howard, hireDate=2018-01-01, annualSalary=105000.0]",
                curlyHoward.toString()
            );
        }

        @Test
        @DisplayName("equals() for identical fields")
        void equalsIdentical() {
            SalariedEmployee copy = new SalariedEmployee("Moe Howard", LocalDate.of(2004, 1, 1), 75000.0);
            assertEquals(moeHoward, copy);
        }

        @Test
        @DisplayName("equals() for different fields")
        void equalsDifferent() {
            assertNotEquals(moeHoward, curlyHoward);
        }

        @Test
        @DisplayName("hashCode() consistent with equals()")
        void hashCodeConsistent() {
            SalariedEmployee copy = new SalariedEmployee("Moe Howard", LocalDate.of(2004, 1, 1), 75000.0);
            assertEquals(moeHoward.hashCode(), copy.hashCode());
        }
    }

    @Nested
    @DisplayName("compareTo and sorting")
    class CompareToTests {

        @Test
        @DisplayName("compareTo() orders by monthly pay ascending")
        void compareToOrdering() {
            assertTrue(moeHoward.compareTo(johnDoe) < 0, "Moe ($6250) < John ($8080)");
            assertTrue(johnDoe.compareTo(curlyHoward) < 0, "John ($8080) < Curly ($8750)");
            assertTrue(curlyHoward.compareTo(janeDoe) < 0, "Curly ($8750) < Jane ($12040)");
        }

        @Test
        @DisplayName("compareTo() returns 0 for equal pay")
        void compareToEqual() {
            SalariedEmployee samePay = new SalariedEmployee("Same Pay", LocalDate.of(2020, 1, 1), 96960.0);
            assertEquals(0, johnDoe.compareTo(samePay), "Both should have $8080/mo");
        }

        @Test
        @DisplayName("Collections.sort() produces correct order")
        void sortOrder() {
            List<Employee> employees = new ArrayList<>();
            employees.add(johnDoe);
            employees.add(janeDoe);
            employees.add(moeHoward);
            employees.add(curlyHoward);

            Collections.sort(employees);

            assertEquals("Moe Howard", employees.get(0).getName());
            assertEquals("John Doe", employees.get(1).getName());
            assertEquals("Curly Howard", employees.get(2).getName());
            assertEquals("Jane Doe", employees.get(3).getName());
        }

        @Test
        @DisplayName("Sorted monthly pay values are in ascending order")
        void sortedPayValues() {
            List<Employee> employees = new ArrayList<>();
            employees.add(johnDoe);
            employees.add(janeDoe);
            employees.add(moeHoward);
            employees.add(curlyHoward);

            Collections.sort(employees);

            assertEquals(6250.0, employees.get(0).getMonthlyPay(), 0.001);
            assertEquals(8080.0, employees.get(1).getMonthlyPay(), 0.001);
            assertEquals(8750.0, employees.get(2).getMonthlyPay(), 0.001);
            assertEquals(12040.0, employees.get(3).getMonthlyPay(), 0.001);
        }

        @Test
        @DisplayName("Total monthly pay is $35,120.00")
        void totalMonthlyPay() {
            List<Employee> employees = List.of(johnDoe, janeDoe, moeHoward, curlyHoward);
            double total = employees.stream().mapToDouble(Employee::getMonthlyPay).sum();
            assertEquals(35120.0, total, 0.001);
        }
    }
}
