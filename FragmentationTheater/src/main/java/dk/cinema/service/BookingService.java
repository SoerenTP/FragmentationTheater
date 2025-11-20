package dk.cinema.service;

import dk.cinema.model.*;
import java.util.*;

/**
 * Service til håndtering af biografsædebookinger med fragmenteringsforebyggelse.
 *
 * Regler:
 * - 1 person må sidde hvor som helst
 * - Alle sæder i en booking skal være i samme række
 * - Grupper må ikke skabe isolerede enkeltsæder
 * - "Sidste udvej": Hvis der er ≤4 ledige sæder tilbage og man booker ≥(ledige-1), tillades det
 * - Alle sæder i samme række skal være sammenhængende
 */
public class BookingService {
    private final CinemaHall cinemaHall;
    private final Map<String, List<Seat>> bookings;
    private int totalBookings;
    private int rejectedBookings;

    /**
     * Standard constructor - opretter 8 rækker x 8 sæder
     */
    public BookingService() {
        this(8, 12);
    }

    /**
     * Konfigurerbar constructor
     *
     * @param rows Antal rækker i biografsalen
     * @param seatsPerRow Antal sæder per række
     */
    public BookingService(int rows, int seatsPerRow) {
        this.cinemaHall = new CinemaHall(rows, seatsPerRow);
        this.bookings = new HashMap<>();
        this.totalBookings = 0;
        this.rejectedBookings = 0;
    }

    public CinemaHall getCinemaHall() {
        return cinemaHall;
    }

    /**
     * Booker sæder hvis de er tilgængelige og ikke skaber fragmentering.
     * Alle sæder skal være i samme række.
     *
     * @param request BookingRequest med seatIds og customerName
     * @return Map med success status, booking ID, eller fejlbesked
     */
    public synchronized Map<String, Object> bookSeats(BookingRequest request) {
        Map<String, Object> result = new HashMap<>();

        // Validér at alle sæder eksisterer og er ledige
        List<Seat> seatsToBook = new ArrayList<>();
        Set<Integer> rows = new HashSet<>();

        for (String seatId : request.getSeatIds()) {
            String[] parts = seatId.split("-");
            int row = Integer.parseInt(parts[0]);
            int number = Integer.parseInt(parts[1]);

            rows.add(row);

            Seat seat = cinemaHall.getSeat(row, number);
            if (seat == null || seat.isOccupied()) {
                result.put("success", false);
                result.put("message", "Plads " + seatId + " er ikke tilgængelig");
                result.put("reason", "SEAT_UNAVAILABLE");
                return result;
            }
            seatsToBook.add(seat);
        }

        // Tjek at alle sæder er i samme række (undtagen for 1 person)
        if (seatsToBook.size() > 1 && rows.size() > 1) {
            result.put("success", false);
            result.put("message", "Alle sæder skal være i samme række");
            result.put("reason", "DIFFERENT_ROWS");
            return result;
        }

        // Tjek at sæderne er sammenhængende i rækken
        if (seatsToBook.size() > 1) {
            List<Integer> seatNumbers = new ArrayList<>();
            for (Seat seat : seatsToBook) {
                seatNumbers.add(seat.getNumber());
            }
            Collections.sort(seatNumbers);

            for (int i = 0; i < seatNumbers.size() - 1; i++) {
                if (seatNumbers.get(i + 1) - seatNumbers.get(i) != 1) {
                    result.put("success", false);
                    result.put("message", "Sæderne skal være sammenhængende");
                    result.put("reason", "NOT_CONTIGUOUS");
                    return result;
                }
            }
        }

        // REGEL 1: 1 person må sidde hvor som helst
        if (seatsToBook.size() == 1) {
            String bookingId = UUID.randomUUID().toString();
            for (Seat seat : seatsToBook) {
                seat.book(bookingId);
            }
            bookings.put(bookingId, seatsToBook);
            totalBookings++;

            result.put("success", true);
            result.put("bookingId", bookingId);
            result.put("message", "Booking gennemført! 1 plads reserveret");
            return result;
        }

        // Tjek om booking ville skabe fragmentering
        FragmentationCheckResult fragmentationCheck = wouldCreateFragmentation(seatsToBook);

        // REGEL 2 + 3: Tjek "sidste udvej" scenario
        int totalAvailableSeats = countAvailableSeats();
        boolean isLastResort = (totalAvailableSeats <= 4 && seatsToBook.size() >= totalAvailableSeats - 1);

        if (fragmentationCheck.wouldFragment && !isLastResort) {
            rejectedBookings++;
            result.put("success", false);
            result.put("message", fragmentationCheck.message);
            result.put("reason", "FRAGMENTATION_PREVENTION");
            result.put("isolatedSeats", fragmentationCheck.isolatedSeats);
            result.put("suggestions", getSuggestedAlternatives(seatsToBook));
            return result;
        }

        double fragmentationBefore = calculateFragmentation();

        // Gennemfør booking
        String bookingId = UUID.randomUUID().toString();
        for (Seat seat : seatsToBook) {
            seat.book(bookingId);
        }
        bookings.put(bookingId, seatsToBook);
        totalBookings++;

        double fragmentationAfter = calculateFragmentation();

        result.put("success", true);
        result.put("bookingId", bookingId);
        result.put("message", "Booking gennemført! " + seatsToBook.size() + " plads(er) reserveret");
        result.put("fragmentationBefore", fragmentationBefore);
        result.put("fragmentationAfter", fragmentationAfter);
        result.put("fragmentationIncrease", fragmentationAfter - fragmentationBefore);

        return result;
    }

    /**
     * Tjekker om en booking ville skabe isolerede enkeltsæder.
     *
     * En plads er isoleret hvis:
     * - Den er ledig
     * - Både venstre OG højre nabo er optaget/kanten
     *
     * @param seatsToBook Liste af sæder der skal bookes
     * @return FragmentationCheckResult med status og isolerede sæder
     */
    private FragmentationCheckResult wouldCreateFragmentation(List<Seat> seatsToBook) {
        FragmentationCheckResult result = new FragmentationCheckResult();
        result.isolatedSeats = new ArrayList<>();

        // Opret temporary set af bookede sæder
        Set<String> tempBookedIds = new HashSet<>();
        for (Seat seat : seatsToBook) {
            tempBookedIds.add(seat.getId());
        }

        // Find berørte rækker
        Set<Integer> affectedRows = new HashSet<>();
        for (Seat seat : seatsToBook) {
            affectedRows.add(seat.getRow());
        }

        // Tjek hver berørt række for isolerede sæder
        for (int row : affectedRows) {
            for (int s = 0; s < cinemaHall.getSeatsPerRow(); s++) {
                Seat seat = cinemaHall.getSeat(row, s);
                String seatId = seat.getId();

                if (tempBookedIds.contains(seatId) || seat.isOccupied()) {
                    continue;
                }

                boolean leftOccupied = isOccupiedOrInBooking(row, s - 1, tempBookedIds);
                boolean rightOccupied = isOccupiedOrInBooking(row, s + 1, tempBookedIds);

                if (leftOccupied && rightOccupied) {
                    result.isolatedSeats.add(seatId);
                }
            }
        }

        if (!result.isolatedSeats.isEmpty()) {
            result.wouldFragment = true;
            if (result.isolatedSeats.size() == 1) {
                result.message = "Denne booking ville efterlade 1 isoleret plads (" +
                        result.isolatedSeats.get(0) + "). Vælg venligst andre pladser.";
            } else {
                result.message = "Denne booking ville efterlade " + result.isolatedSeats.size() +
                        " isolerede pladser (" + String.join(", ", result.isolatedSeats) +
                        "). Vælg venligst andre pladser.";
            }
        }

        return result;
    }

    /**
     * Hjælpefunktion til at tjekke om et sæde er optaget eller del af aktuel booking.
     *
     * @param row Række nummer
     * @param seatNum Sæde nummer
     * @param bookingIds Set af sæder i aktuel booking
     * @return true hvis optaget, false hvis ledig
     */
    private boolean isOccupiedOrInBooking(int row, int seatNum, Set<String> bookingIds) {
        if (seatNum < 0 || seatNum >= cinemaHall.getSeatsPerRow()) {
            return true; // Kant af række = "optaget"
        }

        Seat seat = cinemaHall.getSeat(row, seatNum);
        return seat.isOccupied() || bookingIds.contains(seat.getId());
    }

    /**
     * Finder alternative bookingforslag i samme række.
     *
     * @param requestedSeats De sæder brugeren forsøgte at booke
     * @return Liste af forslag (max 3)
     */
    private List<String> getSuggestedAlternatives(List<Seat> requestedSeats) {
        List<String> suggestions = new ArrayList<>();
        int requestedSize = requestedSeats.size();

        int preferredRow = requestedSeats.get(0).getRow();

        // Prøv alle mulige startpositioner
        for (int startSeat = 0; startSeat <= cinemaHall.getSeatsPerRow() - requestedSize; startSeat++) {
            boolean allAvailable = true;
            List<Seat> candidateSeats = new ArrayList<>();

            // Tjek om blok er ledig
            for (int i = 0; i < requestedSize; i++) {
                Seat seat = cinemaHall.getSeat(preferredRow, startSeat + i);
                if (seat.isOccupied()) {
                    allAvailable = false;
                    break;
                }
                candidateSeats.add(seat);
            }

            if (allAvailable) {
                FragmentationCheckResult check = wouldCreateFragmentation(candidateSeats);
                if (!check.wouldFragment) {
                    suggestions.add("Række " + (preferredRow + 1) + ", pladser " +
                            (startSeat + 1) + "-" + (startSeat + requestedSize));

                    if (suggestions.size() >= 3) break;
                }
            }
        }

        return suggestions;
    }

    /**
     * Tæller antal ledige sæder i hele biografen.
     *
     * @return Antal ledige sæder
     */
    private int countAvailableSeats() {
        int count = 0;
        for (Seat seat : cinemaHall.getAllSeats()) {
            if (!seat.isOccupied()) count++;
        }
        return count;
    }

    /**
     * Returnerer liste af tilgængelige sæder for en given gruppestørrelse.
     * Denne liste bruges af frontend til at farvelægge sæder.
     *
     * @param partySize Antal personer der skal booke
     * @return Liste af sæde IDs der kan bookes uden fragmentering
     */
    public List<String> getAvailableSeatsForBooking(int partySize) {
        List<String> availableSeats = new ArrayList<>();

        // REGEL 1: Party size 1 kan sidde hvor som helst
        if (partySize == 1) {
            for (Seat seat : cinemaHall.getAllSeats()) {
                if (!seat.isOccupied()) {
                    availableSeats.add(seat.getId());
                }
            }
            return availableSeats;
        }

        // For party size > 1, tjek alle mulige sammenhængende blokke
        for (int row = 0; row < cinemaHall.getRows(); row++) {
            for (int startSeat = 0; startSeat <= cinemaHall.getSeatsPerRow() - partySize; startSeat++) {
                // Tjek om denne sammenhængende blok er ledig
                boolean blockAvailable = true;
                List<Seat> candidateSeats = new ArrayList<>();

                for (int i = 0; i < partySize; i++) {
                    Seat seat = cinemaHall.getSeat(row, startSeat + i);
                    if (seat.isOccupied()) {
                        blockAvailable = false;
                        break;
                    }
                    candidateSeats.add(seat);
                }

                if (blockAvailable) {
                    // REGEL 2: Tjek om booking ville skabe problematisk fragmentering
                    FragmentationCheckResult check = wouldCreateFragmentation(candidateSeats);

                    // REGEL 3: Tjek "sidste udvej" scenario
                    int totalAvailable = countAvailableSeats();
                    boolean isLastResort = (totalAvailable <= 4 && partySize >= totalAvailable - 1);

                    if (!check.wouldFragment || isLastResort) {
                        // Tilføj alle sæder i denne gyldige blok
                        for (Seat seat : candidateSeats) {
                            if (!availableSeats.contains(seat.getId())) {
                                availableSeats.add(seat.getId());
                            }
                        }
                    }
                }
            }
        }

        return availableSeats;
    }

    /**
     * Beregner fragmenteringsprocent for hele biografen.
     *
     * Fragmentering = (isolerede sæder / totale ledige sæder) * 100
     *
     * @return Fragmenteringsprocent (0-100)
     */
    public double calculateFragmentation() {
        int isolatedSeats = 0;
        int totalEmptySeats = 0;

        for (int r = 0; r < cinemaHall.getRows(); r++) {
            for (int s = 0; s < cinemaHall.getSeatsPerRow(); s++) {
                Seat seat = cinemaHall.getSeat(r, s);
                if (!seat.isOccupied()) {
                    totalEmptySeats++;

                    boolean leftOccupied = (s == 0) || cinemaHall.getSeat(r, s - 1).isOccupied();
                    boolean rightOccupied = (s == cinemaHall.getSeatsPerRow() - 1) ||
                            cinemaHall.getSeat(r, s + 1).isOccupied();

                    if (leftOccupied && rightOccupied) {
                        isolatedSeats++;
                    }
                }
            }
        }

        if (totalEmptySeats == 0) return 0.0;
        return (isolatedSeats * 100.0) / totalEmptySeats;
    }

    /**
     * Beregner udnyttelsesgrad for biografen.
     *
     * @return Procent af optagne sæder (0-100)
     */
    public double calculateUtilization() {
        int occupied = 0;
        int total = cinemaHall.getRows() * cinemaHall.getSeatsPerRow();

        for (Seat seat : cinemaHall.getAllSeats()) {
            if (seat.isOccupied()) occupied++;
        }

        return (occupied * 100.0) / total;
    }

    /**
     * Henter statistik for biografen.
     *
     * @return Map med statistik
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalBookings", totalBookings);
        stats.put("rejectedBookings", rejectedBookings);
        stats.put("fragmentation", calculateFragmentation());
        stats.put("utilization", calculateUtilization());
        stats.put("totalSeats", cinemaHall.getRows() * cinemaHall.getSeatsPerRow());

        int occupied = 0;
        for (Seat seat : cinemaHall.getAllSeats()) {
            if (seat.isOccupied()) occupied++;
        }
        stats.put("occupiedSeats", occupied);

        return stats;
    }

    /**
     * Nulstiller biografen til tom tilstand.
     */
    public void reset() {
        for (Seat seat : cinemaHall.getAllSeats()) {
            seat.release();
        }
        bookings.clear();
        totalBookings = 0;
        rejectedBookings = 0;
    }

    /**
     * Intern hjælpeklasse til at holde resultat af fragmenteringstjek.
     */
    private static class FragmentationCheckResult {
        boolean wouldFragment = false;
        String message = "";
        List<String> isolatedSeats = new ArrayList<>();
    }
}