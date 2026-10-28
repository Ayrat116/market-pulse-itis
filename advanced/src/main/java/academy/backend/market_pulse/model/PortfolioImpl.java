package academy.backend.market_pulse.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@RequiredArgsConstructor
public class PortfolioImpl implements Portfolio {

    @Getter
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class PositionRecord implements Position {
        private final Instrument instrument;
        private final int quantity;
    }

    @Getter
    private final String name;
    private Position[] positions = new Position[0];

    @Override
    public void addPosition(Instrument instrument, int quantity) {
        positions = Arrays.copyOf(positions, positions.length + 1);
        positions[positions.length - 1] = new PositionRecord(instrument, quantity);
    }

    @Override
    public Position[] getPositions() {
        return positions;
    }
}
