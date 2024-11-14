package io.openliberty.jpa.data.tests.models;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.ZoneId;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Store {

    @Id
    public long purchaseId;

    public String customer;

    public Instant time;

    public static Store of(int year, int month, int day,String customer,long purchaseId) {
        Store inst = new Store();
        inst.purchaseId = purchaseId;
        inst.customer = customer;
        inst.time = ZonedDateTime.of(year, month, day, 12, 0, 0, 0, ZoneId.of("America/New_York")).toInstant();
        return inst;
    }
}