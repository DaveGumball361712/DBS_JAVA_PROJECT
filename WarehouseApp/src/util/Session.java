package util;

import model.Employee;

public class Session {
    public static Employee currentUser = null;

    public static void login(Employee user) {
        currentUser = user;
    }

    public static void logout() {
        currentUser = null;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }
}
