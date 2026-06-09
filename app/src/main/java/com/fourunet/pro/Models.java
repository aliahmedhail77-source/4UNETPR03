package com.fourunet.pro;

class CategoryItem {
    String id;
    int amount;
    String name;
    boolean active;
    long createdAt;

    CategoryItem(String id, int amount, String name, boolean active, long createdAt) {
        this.id = id;
        this.amount = amount;
        this.name = name;
        this.active = active;
        this.createdAt = createdAt;
    }
}

class CardItem {
    String id;
    int amount;
    String code;
    boolean sold;
    String buyerPhone;
    String soldAt;
    String source;

    CardItem(String id, int amount, String code, boolean sold, String buyerPhone, String soldAt, String source) {
        this.id = id;
        this.amount = amount;
        this.code = code;
        this.sold = sold;
        this.buyerPhone = buyerPhone;
        this.soldAt = soldAt;
        this.source = source;
    }
}

class OperationLog {
    String id;
    String provider;
    String sender;
    String customerName;
    String customerPhone;
    int amount;
    String status;
    String message;
    String cardCode;
    String createdAt;

    OperationLog(String id, String provider, String sender, String customerName, String customerPhone, int amount, String status, String message, String cardCode, String createdAt) {
        this.id = id;
        this.provider = provider;
        this.sender = sender;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.amount = amount;
        this.status = status;
        this.message = message;
        this.cardCode = cardCode;
        this.createdAt = createdAt;
    }
}

class ParsedPayment {
    String provider;
    int amount;
    String customerPhone;
    String customerName;

    ParsedPayment(String provider, int amount, String customerPhone, String customerName) {
        this.provider = provider;
        this.amount = amount;
        this.customerPhone = customerPhone;
        this.customerName = customerName;
    }
}

class TrustedContact {
    String id;
    String fullName;
    String tripleName;
    String phone;
    boolean active;

    TrustedContact(String id, String fullName, String tripleName, String phone, boolean active) {
        this.id = id;
        this.fullName = fullName;
        this.tripleName = tripleName;
        this.phone = phone;
        this.active = active;
    }
}
