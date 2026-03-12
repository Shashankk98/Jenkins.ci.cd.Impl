package org.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

class MainTest {

    @Test
    @DisplayName("greet returns proper greeting message")
    void testGreet() {
        String result = Main.greet("Shashank");
        assertEquals("Hello, Shashank! Welcome to CI/CD Pipeline Demo.", result);
    }

    @Test
    @DisplayName("greet with empty name")
    void testGreetEmptyName() {
        String result = Main.greet("");
        assertEquals("Hello, ! Welcome to CI/CD Pipeline Demo.", result);
    }

    @Test
    @DisplayName("add two positive numbers")
    void testAdd() {
        assertEquals(5, Main.add(2, 3));
    }

    @Test
    @DisplayName("add with negative numbers")
    void testAddNegative() {
        assertEquals(-1, Main.add(2, -3));
    }

    @Test
    @DisplayName("add with zero")
    void testAddZero() {
        assertEquals(0, Main.add(0, 0));
    }

    @Test
    @DisplayName("factorial of 0 is 1")
    void testFactorialZero() {
        assertEquals(1, Main.factorial(0));
    }

    @Test
    @DisplayName("factorial of 5 is 120")
    void testFactorialFive() {
        assertEquals(120, Main.factorial(5));
    }

    @Test
    @DisplayName("factorial of 1 is 1")
    void testFactorialOne() {
        assertEquals(1, Main.factorial(1));
    }

    @Test
    @DisplayName("factorial throws on negative input")
    void testFactorialNegative() {
        assertThrows(IllegalArgumentException.class, () -> Main.factorial(-1));
    }

    @Test
    @DisplayName("main method runs without exception")
    void testMain() {
        assertDoesNotThrow(() -> Main.main(new String[]{}));
    }
}

