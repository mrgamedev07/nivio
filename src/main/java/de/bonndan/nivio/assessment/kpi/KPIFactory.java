package de.bonndan.nivio.assessment.kpi;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.bonndan.nivio.model.Lifecycle;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Factory for custom and builtin KPIs.
 */
public class KPIFactory extends JsonDeserializer<Map<String, KPI>> implements Serializable {

    @Override
    public Map<String, KPI> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {

        JsonNode node = jsonParser.getCodec().readTree(jsonParser);

        ObjectMapper mapper = new ObjectMapper();
        Map<String, Map<String, String>> result = mapper.convertValue(node, new TypeReference<Map<String, Map<String, ?>>>() {
        });

        Map<String, KPI> kpis = new HashMap<>();
        result.forEach((s, params) -> {
            KPI kpi = null;
            if (s.equals(HealthKPI.IDENTIFIER)) {
                kpi = mapper.convertValue(params, HealthKPI.class);
                Objects.requireNonNull(kpi);
            }

            if (s.equals(ScalingKPI.IDENTIFIER)) {
                kpi = mapper.convertValue(params, ScalingKPI.class);
                Objects.requireNonNull(kpi);
            }

            if (s.equals(ConditionKPI.IDENTIFIER)) {
                kpi = new ConditionKPI();
            }

            if (s.equals(LifecycleKPI.IDENTIFIER)) {
                kpi = new LifecycleKPI();
            }

            if (kpi == null) {
                kpi = mapper.convertValue(params, CustomKPI.class);
            }
            kpis.put(s, kpi);
        });

        return kpis;
    }
}