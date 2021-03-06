package net.floodlightcontroller.statistics.web;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.floodlightcontroller.statistics.SwitchPortBandwidth;

import java.io.IOException;
import java.util.Date;

public class SwitchPortBandwidthSerializer extends JsonSerializer<SwitchPortBandwidth> {

	@Override
	public void serialize(SwitchPortBandwidth spb, JsonGenerator jGen, SerializerProvider serializer) throws IOException, JsonProcessingException {
		jGen.configure(Feature.WRITE_NUMBERS_AS_STRINGS, true);

		jGen.writeStartObject();
		jGen.writeStringField("dpid", spb.getSwitchId().toString());
		jGen.writeStringField("port", spb.getSwitchPort().toString());
		jGen.writeStringField("updated", new Date(spb.getUpdateTime()).toString());
		jGen.writeStringField("link-speed-bits-per-second", spb.getLinkSpeedBitsPerSec().getBigInteger().toString());
		jGen.writeStringField("bits-per-second-rx", spb.getBitsPerSecondRx().getBigInteger().toString());
		jGen.writeStringField("bits-per-second-tx", spb.getBitsPerSecondTx().getBigInteger().toString());
		jGen.writeNumberField("tx-utilization", spb.getTxUtilization());
		jGen.writeNumberField("tx-utilization-percent", spb.getTxUtilizationPercent());
		jGen.writeNumberField("tx-available-bandwidth", spb.getAvailableTxBandwidth());
		jGen.writeNumberField("rx-utilization", spb.getRxUtilization());
		jGen.writeNumberField("rx-utilization-percent", spb.getRxUtilizationPercent());
		jGen.writeNumberField("rx-available-bandwidth", spb.getAvailableRxBandwidth());
		jGen.writeEndObject();
	}

}