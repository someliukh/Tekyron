package com.tekyron.example;

import com.tekyron.core.TekyronApplication;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        TekyronApplication.launch("http://127.0.0.1:5173");
    }
}