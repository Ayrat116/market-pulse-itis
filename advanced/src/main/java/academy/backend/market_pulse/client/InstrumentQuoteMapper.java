package academy.backend.market_pulse.client;

import academy.backend.market_pulse.dictionary.InstrumentType;
import academy.backend.market_pulse.dto.InstrumentQuoteDto;
import academy.backend.market_pulse.exception.TInvestApiException;
import academy.backend.market_pulse.factory.InstrumentFactories;
import academy.backend.market_pulse.model.Currency;
import academy.backend.market_pulse.model.Instrument;
import academy.backend.market_pulse.model.Quote;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

/**
 * Общий маппинг {@link InstrumentQuoteDto} → {@link Quote} для всех реализаций
 * {@link TInvestApiClient} (см. пакет {@code client.impl}) — способ получения ответа у них разный,
 * а разбор JSON и восстановление домена из DTO одно и то же.
 */
public final class InstrumentQuoteMapper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private InstrumentQuoteMapper() {
    }

    /** Разбор тела ответа {@code instruments-service} (JSON-массив) в котировки. */
    public static List<Quote> fromJson(String body) {
        try {
            List<InstrumentQuoteDto> dtos = OBJECT_MAPPER.readValue(body, OBJECT_MAPPER.getTypeFactory()
                    .constructCollectionType(List.class, InstrumentQuoteDto.class));
            return dtos.stream().map(InstrumentQuoteMapper::toQuote).toList();
        } catch (IOException e) {
            throw new TInvestApiException("Не удалось разобрать ответ instruments-service", e);
        }
    }

    public static Quote toQuote(InstrumentQuoteDto dto) {
        Instrument instrument = InstrumentFactories.create(
                InstrumentType.valueOf(dto.getType()), dto.getTicker(), dto.getName(),
                Currency.valueOf(dto.getCurrency()));
        return new Quote(instrument, dto.getPrice(), dto.getChangePercent());
    }
}
