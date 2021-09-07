package org.mtransit.parser.ca_niagara_falls_wego_bus;

import static org.mtransit.parser.StringUtils.EMPTY;

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
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// https://niagaraopendata.ca/dataset/niagara-region-transit-gtfs
// https://maps.niagararegion.ca/googletransit/NiagaraRegionTransit.zip
// https://niagaraopendata.ca/dataset/1a1b885e-1a86-415d-99aa-6803a2d8f178/resource/f7dbcaed-f31a-435e-8146-b0efff0b8eb8/download/gtfs.zip
public class NiagaraFallsWEGOBusAgencyTools extends DefaultAgencyTools {

	public static void main(@NotNull String[] args) {
		new NiagaraFallsWEGOBusAgencyTools().start(args);
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
					"red"
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
	public long getRouteId(@NotNull GRoute gRoute) {
		//noinspection deprecation
		String routeId = gRoute.getRouteId();
		//noinspection deprecation
		if (!gRoute.getAgencyIdOrDefault().startsWith("AllNRT_")) {
			routeId = STARTS_WITH_WEGO_NF_A00.matcher(routeId).replaceAll(EMPTY);
			Matcher matcher = DIGITS.matcher(routeId);
			if (matcher.find()) {
				return Long.parseLong(matcher.group());
			}
		}
		if (RSN_RED.equalsIgnoreCase(gRoute.getRouteShortName())) {
			return RID_RED;
		} else if (RSN_BLUE.equalsIgnoreCase(gRoute.getRouteShortName())) {
			return RID_BLUE;
		} else if (RSN_GREEN.equalsIgnoreCase(gRoute.getRouteShortName())) {
			return RID_GREEN;
		} else if (RSN_ORANGE.equalsIgnoreCase(gRoute.getRouteShortName())) {
			return RID_ORANGE;
		}
		throw new MTLog.Fatal("Unexpected route ID for %s!", gRoute);
	}

	@Nullable
	@Override
	public String getRouteShortName(@NotNull GRoute gRoute) {
		if (CharUtils.isDigitsOnly(gRoute.getRouteShortName())) {
			int rsn = Integer.parseInt(gRoute.getRouteShortName());
			switch (rsn) {
			// @formatter:off
			case 300: return RSN_RED; // Red
			case 601: return RSN_RED; // Red
			case (int) RID_BLUE: return RSN_BLUE; // Blue
			case (int) RID_GREEN: return RSN_GREEN; // Green
			case (int) RID_ORANGE: return RSN_ORANGE; // Orange
			// @formatter:on
			default:
				throw new MTLog.Fatal("Unexpected route short name for %s!", gRoute);
			}
		}
		return super.getRouteShortName(gRoute); // used by real-time API
	}

	private static final String FALLSVIEW_CLIFTON_HL = "Fallsview / Clifton Hl";
	private static final String LUNDY_S_LN = "Lundy's Ln";
	private static final String NIAGARA_PKS = "Niagara Pks";
	private static final String NOTL_SHUTTLE = "NOTL Shuttle";

	private static final String RSN_BLUE = "BLUE";
	private static final String RSN_GREEN = "GREEN";
	private static final String RSN_ORANGE = "ORANGE";
	private static final String RSN_PRPLE = "Prple";
	private static final String RSN_PURPLE = "Purple";
	private static final String RSN_RED = "RED";

	private static final long RID_RED = 601L;
	private static final long RID_BLUE = 602L;
	private static final long RID_GREEN = 603L;
	private static final long RID_ORANGE = 604L;

	@NotNull
	@Override
	public String getRouteLongName(@NotNull GRoute gRoute) {
		if (CharUtils.isDigitsOnly(gRoute.getRouteShortName())) {
			int rsn = Integer.parseInt(gRoute.getRouteShortName());
			switch (rsn) {
			// @formatter:off
			case 601: return LUNDY_S_LN; // Red
			case (int) RID_BLUE: return FALLSVIEW_CLIFTON_HL; // Blue
			case (int) RID_GREEN: return NIAGARA_PKS; // Green
			case (int) RID_ORANGE: return NOTL_SHUTTLE; // Orange
			// @formatter:on
			default:
				throw new MTLog.Fatal("Unexpected route long name for %s!", gRoute);
			}
		}
		if (RSN_BLUE.equalsIgnoreCase(gRoute.getRouteShortName())) {
			return FALLSVIEW_CLIFTON_HL;
		} else if (RSN_GREEN.equalsIgnoreCase(gRoute.getRouteShortName())) {
			return NIAGARA_PKS;
		} else if (RSN_ORANGE.equalsIgnoreCase(gRoute.getRouteShortName())) {
			return NOTL_SHUTTLE;
		} else if (RSN_RED.equalsIgnoreCase(gRoute.getRouteShortName())) {
			return LUNDY_S_LN;
		}
		throw new MTLog.Fatal("Unexpected route long name for %s!", gRoute);
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
	public String getRouteColor(@NotNull GRoute gRoute) {
		if (StringUtils.isEmpty(gRoute.getRouteColor())) {
			if (CharUtils.isDigitsOnly(gRoute.getRouteShortName())) {
				int rsn = Integer.parseInt(gRoute.getRouteShortName());
				switch (rsn) {
				// @formatter:off
				case (int) RID_RED:return "EE1E23"; // Red
				case (int) RID_BLUE:return "5484CC"; // Blue
				case (int) RID_GREEN:return "45BA67"; // Green
				case (int) RID_ORANGE: return null; // same as agency // Orange
				// @formatter:on
				default:
					throw new MTLog.Fatal("Unexpected route color for %s!", gRoute);
				}
			}
			if (RSN_BLUE.equalsIgnoreCase(gRoute.getRouteShortName())) {
				return "5484CC";
			} else if (RSN_GREEN.equalsIgnoreCase(gRoute.getRouteShortName())) {
				return "45BA67";
			} else if (RSN_ORANGE.equalsIgnoreCase(gRoute.getRouteShortName())) {
				return null; // same as agency
			} else if (RSN_PURPLE.equalsIgnoreCase(gRoute.getRouteShortName())
					|| RSN_PRPLE.equalsIgnoreCase(gRoute.getRouteShortName())) {
				return "7040A4";
			} else if (RSN_RED.equalsIgnoreCase(gRoute.getRouteShortName())) {
				return "EE1E23";
			}
			throw new MTLog.Fatal("Unexpected route color for %s!", gRoute);
		}
		return super.getRouteColor(gRoute);
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

	private static final Pattern STARTS_WITH_BOUNDS_SLASH = Pattern.compile("(^(.* )?(inbound|outbound)(/))", Pattern.CASE_INSENSITIVE);

	private static final Pattern STARTS_WITH_RSN_ = Pattern.compile("(^[\\d]+( )?)", Pattern.CASE_INSENSITIVE);
	private static final Pattern STARTS_WITH_RLN_DASH = Pattern.compile("(^[^\\-]+-)", Pattern.CASE_INSENSITIVE);

	private static final Pattern AND_NO_SPACE = Pattern.compile("(([\\S])\\s?([&@])\\s?([\\S]))", Pattern.CASE_INSENSITIVE);
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

	private static final Pattern STARTS_WITH_WEGO_NF_A00 = Pattern.compile("((^)((wego|nf|nft|allnrt)_[a-z]{1,3}[\\d]{2,4}(_)?)+(stop|sto)?)",
			Pattern.CASE_INSENSITIVE);

	// STOP CODE REQUIRED FOR REAL-TIME API
	@NotNull
	@Override
	public String getStopCode(@NotNull GStop gStop) {
		String stopCode = gStop.getStopCode();
		if (stopCode.length() == 0 || ZERO_0.equals(stopCode)) {
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

	private static final Pattern DIGITS = Pattern.compile("[\\d]+");

	@Override
	public int getStopId(@NotNull GStop gStop) {
		String stopCode = gStop.getStopCode();
		if (stopCode.length() == 0 || ZERO_0.equals(stopCode)) {
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
