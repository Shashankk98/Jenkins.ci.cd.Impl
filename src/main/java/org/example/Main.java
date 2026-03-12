package org.example;

public class Main {

    public static String greet(String name) {
        return "Hello, " + name + "! Welcome to CI/CD Pipeline Demo.";
    }

    public static int add(int a, int b) {
        return a + b;
    }

    public static int factorial(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("Negative numbers not allowed");
        }
        if (n <= 1) {
            return 1;
        }
        return n * factorial(n - 1);
    }

    public static void main(String[] args) {
        System.out.println(greet("Developer"));

        for (int i = 1; i <= 5; i++) {
            System.out.println(i + "! = " + factorial(i));
        }
    }
}
