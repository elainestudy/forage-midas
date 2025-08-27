package com.jpmc.midascore.entity;

import jakarta.persistence.*;

@Entity
public class TransactionRecord {
    @Id
    @GeneratedValue()
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)  // 多条交易对应同一个 sender
    @JoinColumn(name = "sender_id", nullable = false)     // 外键列名
    private UserRecord sender;

    @ManyToOne(fetch = FetchType.LAZY)  // 多条交易对应同一个 recipient
    @JoinColumn(name = "recipient_id", nullable = false)  // 外键列名
    private UserRecord recipient;

    @Column(nullable = false)
    private float amount;

    public TransactionRecord() {

    }

    public TransactionRecord(UserRecord sender, UserRecord recipient, float amount) {
        this.sender = sender;
        this.recipient = recipient;
        this.amount = amount;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public UserRecord getSender() {
        return sender;
    }

    public void setSender(UserRecord sender) {
        this.sender = sender;
    }

    public UserRecord getRecipient() {
        return recipient;
    }

    public void setRecipient(UserRecord recipient) {
        this.recipient = recipient;
    }

    public float getAmount() {
        return amount;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }

    @Override
    public String toString() {
        return "TransactionRecord{" +
                "id=" + id +
                ", senderId=" + (sender != null ? sender.getId() : null) +
                ", recipientId=" + (recipient != null ? recipient.getId() : null) +
                ", amount=" + amount +
                '}';
    }
}
