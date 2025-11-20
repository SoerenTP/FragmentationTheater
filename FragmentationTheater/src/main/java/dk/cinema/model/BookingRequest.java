package dk.cinema.model;

import java.util.List;

public class BookingRequest {
    private List<String> seatIds;
    private String customerName;

    public BookingRequest() {}

    public BookingRequest(List<String> seatIds, String customerName) {
        this.seatIds = seatIds;
        this.customerName = customerName;
    }

    public List<String> getSeatIds() { return seatIds; }
    public void setSeatIds(List<String> seatIds) { this.seatIds = seatIds; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
}