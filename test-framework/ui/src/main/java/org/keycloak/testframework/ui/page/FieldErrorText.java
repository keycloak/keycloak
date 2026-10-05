package org.keycloak.testframework.ui.page;

import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;

final class FieldErrorText {

    private FieldErrorText() {
    }

    static String readNow(ManagedWebDriver driver, String fieldErrorId) {
        try {
            return readFromElement(driver.findElement(By.id(fieldErrorId)));
        } catch (NoSuchElementException e) {
            return "";
        }
    }

    static String read(ManagedWebDriver driver, String fieldErrorId) {
        try {
            return driver.waiting().until(d -> {
                try {
                    String text = readFromElement(d.findElement(By.id(fieldErrorId)));
                    return text.isBlank() ? null : text;
                } catch (NoSuchElementException e) {
                    return null;
                }
            });
        } catch (TimeoutException e) {
            return readNow(driver, fieldErrorId);
        }
    }

    private static String readFromElement(WebElement errorElement) {
        try {
            WebElement feedback = errorElement.findElement(By.className("kc-feedback-text"));
            String text = feedback.getText();
            if (text == null || text.isBlank()) {
                text = feedback.getAttribute("textContent");
            }
            if (text != null && !text.isBlank()) {
                return text.trim();
            }
        } catch (NoSuchElementException ignored) {
        }

        String text = errorElement.getText();
        if (text == null || text.isBlank()) {
            text = errorElement.getAttribute("textContent");
        }
        return text == null ? "" : text.trim();
    }
}
