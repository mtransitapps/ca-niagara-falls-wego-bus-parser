package org.mtransit.parser.ca_niagara_falls_wego_bus;

import static org.mtransit.commons.StringUtils.EMPTY;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.commons.CharUtils;
import org.mtransit.commons.CleanUtils;
import org.mtransit.commons.StringUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.gtfs.data.GAgency;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.mt.data.MAgency;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

// https://niagaraopendata.ca/dataset/niagara-region-transit-gtfs
public class NiagaraFallsWEGOBusAgencyTools extends DefaultAgencyTools {

	public static void main(@NotNull String[] args) {
		new NiagaraFallsWEGOBusAgencyTools().start(args);
	}

	@Nullable
	@Override
	public List<Locale> getSupportedLanguages() {
		return LANG_EN;
	}

	@Override
	public boolean defaultExcludeEnabled() {
		return true;
	}

	@NotNull
	@Override
	public String getAgencyName() {
		return "WEGO";
	}

	@Override
	public boolean excludeAgency(@NotNull GAgency gAgency) {
		//noinspection deprecation
		final String agencyId = gAgency.getAgencyId();
		if (!agencyId.contains("Niagara Parks Commission WeGo") //
				&& !agencyId.contains("Niagara Falls Transit") //
				&& !agencyId.contains("Niagara Falls Transit & WEGO") //
				&& !agencyId.contains("Niagara Falls Transit & WeGo") //
				&& !agencyId.startsWith("AllNRT_")
				&& !agencyId.equals("1")) {
			return EXCLUDE;
		}
		return super.excludeAgency(gAgency);
	}

	@Override
	public boolean excludeRoute(@NotNull GRoute gRoute) {
		//noinspection deprecation
		final String agencyId = gRoute.getAgencyIdOrDefault();
		if (!agencyId.contains("Niagara Parks Commission WeGo") //
				&& !agencyId.contains("Niagara Falls Transit") //
				&& !agencyId.contains("Niagara Falls Transit & WEGO") //
				&& !agencyId.contains("Niagara Falls Transit & WeGo") //
				&& !agencyId.startsWith("AllNRT_")
				&& !agencyId.equals("1")) {
			return EXCLUDE;
		}
		if (agencyId.startsWith("AllNRT_")
				|| agencyId.equals("1")) {
			//noinspection RedundantIfStatement
			if (!Arrays.asList(
					"blue",
					"green",
					"orng", // orange",
					"red",
					"redx"
			).contains(gRoute.getRouteShortName().toLowerCase(Locale.ENGLISH))) {
				return EXCLUDE;
			}
			return KEEP;
		}
		//noinspection deprecation
		final String routeId = gRoute.getRouteId();
		final String routeLongName = gRoute.getRouteLongNameOrDefault();
		if (!routeId.contains("WEGO") //
				&& !routeLongName.contains("WEGO") //
				&& !routeLongName.equals("604 - Orange - NOTL")) {
			return EXCLUDE;
		}
		return super.excludeRoute(gRoute);
	}

	@NotNull
	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	@Override
	public boolean defaultRouteIdEnabled() {
		return true;
	}

	@Override
	public boolean useRouteShortNameForRouteId() {
		return true;
	}

	@Nullable
	@Override
	public Long convertRouteIdFromShortNameNotSupported(@NotNull String routeShortName) {
		switch (routeShortName) {
		case "RED":
			return 601L;
		case "BLUE":
			return 602L;
		case "GREEN":
			return 603L;
		case "ORANGE":
			return 604L;
		case "REDX":
			return 605L;
		}
		throw new MTLog.Fatal("Unexpected route ID for short name '%s'!", routeShortName);
	}

	@Override
	public boolean defaultRouteLongNameEnabled() {
		return true;
	}

	private static final String AGENCY_COLOR_ORANGE = "F3632A"; // ORANGE (from PDF)

	private static final String AGENCY_COLOR = AGENCY_COLOR_ORANGE;

	@Override
	public boolean defaultAgencyColorEnabled() {
		return true;
	}

	@NotNull
	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	@Nullable
	@Override
	public String provideMissingRouteColor(@NotNull GRoute gRoute) {
		final String rsnS = gRoute.getRouteShortName();
		switch (rsnS) {
		case "RED":
		case "601":
			return "EE1E23"; // Red
		case "BLUE":
		case "602":
			return "5484CC"; // Blue
		case "GREEN":
		case "603":
			return "45BA67"; // Green
		case "ORANGE":
		case "604":
			return null; // same as agency // Orange
		case "REDX":
		case "605":
			return "F1836C"; // Salmon
		case "Purple":
		case "Prple":
			return "7040A4";
		}
		throw new MTLog.Fatal("Unexpected route color for %s!", gRoute);
	}

	@NotNull
	@Override
	public String cleanStopOriginalId(@NotNull String gStopId) {
		gStopId = STARTS_WITH_WEGO_NF_A00.matcher(gStopId).replaceAll(StringUtils.EMPTY);
		return gStopId;
	}

	@Override
	public boolean directionFinderEnabled() {
		return true;
	}

	private static final Pattern STARTS_WITH_FROM_VIA_DASH = Pattern.compile("(^[^\\-]+-)", Pattern.CASE_INSENSITIVE);

	@Override
	public @NotNull String cleanDirectionHeadsign(int directionId, boolean fromStopName, @NotNull String directionHeadSign) {
		String directionHeadsign = super.cleanDirectionHeadsign(directionId, fromStopName, directionHeadSign);
		directionHeadsign = STARTS_WITH_FROM_VIA_DASH.matcher(directionHeadsign).replaceAll(EMPTY);
		return directionHeadsign;
	}

	private static final Pattern STARTS_WITH_BOUNDS_SLASH = Pattern.compile("(^(.* )?(inbound|outbound)(/))", Pattern.CASE_INSENSITIVE);

	private static final Pattern STARTS_WITH_RSN_ = Pattern.compile("(^\\d+( )?)", Pattern.CASE_INSENSITIVE);
	private static final Pattern STARTS_WITH_RLN_DASH = Pattern.compile("(^[^\\-]+-)", Pattern.CASE_INSENSITIVE);

	private static final Pattern AND_NO_SPACE = Pattern.compile("((\\S)\\s?([&@])\\s?(\\S))", Pattern.CASE_INSENSITIVE);
	private static final String AND_NO_SPACE_REPLACEMENT = "$2 $3 $4";

	@NotNull
	@Override
	public String cleanTripHeadsign(@NotNull String tripHeadsign) {
		tripHeadsign = CleanUtils.toLowerCaseUpperCaseWords(Locale.ENGLISH, tripHeadsign, getIgnoredWords());
		tripHeadsign = AND_NO_SPACE.matcher(tripHeadsign).replaceAll(AND_NO_SPACE_REPLACEMENT);
		tripHeadsign = STARTS_WITH_RSN_.matcher(tripHeadsign).replaceAll(EMPTY);
		tripHeadsign = STARTS_WITH_RLN_DASH.matcher(tripHeadsign).replaceAll(EMPTY);
		tripHeadsign = STARTS_WITH_BOUNDS_SLASH.matcher(tripHeadsign).replaceAll(EMPTY);
		tripHeadsign = CleanUtils.cleanBounds(tripHeadsign);
		tripHeadsign = CleanUtils.keepToAndRemoveVia(tripHeadsign);
		tripHeadsign = CleanUtils.cleanNumbers(tripHeadsign);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	private String[] getIgnoredWords() {
		return new String[]{
				"NE", "NW", "SE", "SW",
				"NF", "GO", "VIA",
		};
	}

	@NotNull
	@Override
	public String cleanStopName(@NotNull String gStopName) {
		gStopName = AND_NO_SPACE.matcher(gStopName).replaceAll(AND_NO_SPACE_REPLACEMENT);
		gStopName = CleanUtils.toLowerCaseUpperCaseWords(Locale.ENGLISH, gStopName, getIgnoredWords());
		gStopName = CleanUtils.cleanBounds(gStopName);
		gStopName = CleanUtils.cleanNumbers(gStopName);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}

	private static final String ZERO_0 = "0";

	private static final Pattern STARTS_WITH_WEGO_NF_A00 = Pattern.compile("((^)((wego|nf|nft|allnrt)_[a-z]{1,3}\\d{2,4}(_)?)+(stop|sto)?)",
			Pattern.CASE_INSENSITIVE);

	// STOP CODE REQUIRED FOR REAL-TIME API
	@NotNull
	@Override
	public String getStopCode(@NotNull GStop gStop) {
		String stopCode = gStop.getStopCode();
		if (stopCode.isEmpty() || ZERO_0.equals(stopCode)) {
			//noinspection deprecation
			stopCode = gStop.getStopId();
		}
		stopCode = STARTS_WITH_WEGO_NF_A00.matcher(stopCode).replaceAll(EMPTY);
		if ("TablRock".equals(stopCode)) {
			return "8871";
		}
		if ("Sta&6039".equalsIgnoreCase(stopCode)) {
			return EMPTY;
		}
		if (StringUtils.isEmpty(stopCode)) {
			throw new MTLog.Fatal("Unexptected stop code for %s!", gStop);
		}
		return stopCode;
	}

	@Override
	public int getStopId(@NotNull GStop gStop) {
		String stopCode = gStop.getStopCode();
		if (stopCode.isEmpty() || ZERO_0.equals(stopCode)) {
			//noinspection deprecation
			stopCode = gStop.getStopId();
		}
		stopCode = STARTS_WITH_WEGO_NF_A00.matcher(stopCode).replaceAll(StringUtils.EMPTY);
		if ("TablRock".equals(stopCode)) {
			return 8871;
		}
		if (CharUtils.isDigitsOnly(stopCode)) {
			return Integer.parseInt(stopCode); // using stop code as stop ID
		}
		if ("MAR".equalsIgnoreCase(stopCode)) {
			return 900_000;
		} else if ("8CD1".equalsIgnoreCase(stopCode)) {
			return 900_001;
		} else if ("SCT1".equalsIgnoreCase(stopCode)) {
			return 900_002;
		} else if ("SCT2".equalsIgnoreCase(stopCode)) {
			return 900_003;
		} else if ("Sta&6039".equalsIgnoreCase(stopCode)) {
			return 900_004;
		} else if ("Sta&6683".equalsIgnoreCase(stopCode)) {
			return 900_005;
		} else if ("FV&6455".equalsIgnoreCase(stopCode)) {
			return 900_006;
		} else if ("FV&6760".equalsIgnoreCase(stopCode)) {
			return 900_007;
		}
		throw new MTLog.Fatal("Unexpected stop ID %s!", gStop);
	}
}
