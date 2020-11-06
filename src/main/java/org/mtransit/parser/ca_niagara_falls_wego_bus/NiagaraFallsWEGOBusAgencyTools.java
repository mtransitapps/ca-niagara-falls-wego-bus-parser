package org.mtransit.parser.ca_niagara_falls_wego_bus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.Pair;
import org.mtransit.parser.SplitUtils;
import org.mtransit.parser.SplitUtils.RouteTripSpec;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.gtfs.data.GTripStop;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MDirectionType;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTrip;
import org.mtransit.parser.mt.data.MTripStop;

// https://niagaraopendata.ca/dataset/niagara-region-transit-gtfs
// https://niagaraopendata.ca/dataset/niagara-region-transit-gtfs/resource/cc2fda23-0cab-40b7-b264-1cdb01e08fea
// https://maps.niagararegion.ca/googletransit/NiagaraRegionTransit.zip
public class NiagaraFallsWEGOBusAgencyTools extends DefaultAgencyTools {

	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-niagara-falls-wego-bus-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new NiagaraFallsWEGOBusAgencyTools().start(args);
	}

	private HashSet<String> serviceIds;

	@Override
	public void start(String[] args) {
		MTLog.log("Generating WEGO bus data...");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this, true);
		super.start(args);
		MTLog.log("Generating WEGO bus data... DONE in %s.", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludingAll() {
		return this.serviceIds != null && this.serviceIds.isEmpty();
	}

	@Override
	public boolean excludeCalendar(GCalendar gCalendar) {
		if (this.serviceIds != null) {
			return excludeUselessCalendar(gCalendar, this.serviceIds);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(GCalendarDate gCalendarDates) {
		if (this.serviceIds != null) {
			return excludeUselessCalendarDate(gCalendarDates, this.serviceIds);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	@Override
	public boolean excludeTrip(GTrip gTrip) {
		if (this.serviceIds != null) {
			return excludeUselessTrip(gTrip, this.serviceIds);
		}
		return super.excludeTrip(gTrip);
	}

	@Override
	public boolean excludeRoute(GRoute gRoute) {
		if (!gRoute.getAgencyId().contains("Niagara Parks Commission WeGo") //
				&& !gRoute.getAgencyId().contains("Niagara Falls Transit") //
				&& !gRoute.getAgencyId().contains("Niagara Falls Transit & WEGO") //
				&& !gRoute.getAgencyId().contains("Niagara Falls Transit & WeGo") //
				&& !gRoute.getAgencyId().startsWith("AllNRT_")) {
			return true; // excluded
		}
		if (gRoute.getAgencyId().startsWith("AllNRT_")) {
			if (!Arrays.asList(
				"blue",
				"green",
				"red"
			).contains(gRoute.getRouteShortName().toLowerCase(Locale.ENGLISH))) {
					return true; // exclude
			}
			return false; // keep
		}
		if (!gRoute.getRouteId().contains("WEGO") //
				&& !gRoute.getRouteLongName().contains("WEGO") //
				&& !gRoute.getRouteLongName().equals("604 - Orange - NOTL")) {
			return true; // excluded
		}
		return super.excludeRoute(gRoute);
	}

	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	@Override
	public long getRouteId(GRoute gRoute) {
		String routeId = gRoute.getRouteId();
		if (!gRoute.getAgencyId().startsWith("AllNRT_")) {
			routeId = STARTS_WITH_WEGO_NF_A00.matcher(routeId).replaceAll(StringUtils.EMPTY);
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

	@Override
	public String getRouteShortName(GRoute gRoute) {
		if (Utils.isDigitsOnly(gRoute.getRouteShortName())) {
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

	@Override
	public String getRouteLongName(GRoute gRoute) {
		if (Utils.isDigitsOnly(gRoute.getRouteShortName())) {
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
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	private static final String COLOR_5484CC = "5484CC";
	private static final String COLOR_45BA67 = "45BA67";
	private static final String COLOR_7040A4 = "7040A4";
	private static final String COLOR_EE1E23 = "EE1E23";

	@Override
	public String getRouteColor(GRoute gRoute) {
		if (Utils.isDigitsOnly(gRoute.getRouteShortName())) {
			int rsn = Integer.parseInt(gRoute.getRouteShortName());
			switch (rsn) {
			// @formatter:off
			case (int) RID_RED: return COLOR_EE1E23; // Red
			case (int) RID_BLUE: return COLOR_5484CC; // Blue
			case (int) RID_GREEN: return COLOR_45BA67; // Green
			case (int) RID_ORANGE: return null; // same as agency // Orange
			// @formatter:on
			default:
				throw new MTLog.Fatal("Unexpected route color for %s!", gRoute);
			}
		}
		if (RSN_BLUE.equalsIgnoreCase(gRoute.getRouteShortName())) {
			return COLOR_5484CC;
		} else if (RSN_GREEN.equalsIgnoreCase(gRoute.getRouteShortName())) {
			return COLOR_45BA67;
		} else if (RSN_ORANGE.equalsIgnoreCase(gRoute.getRouteShortName())) {
			return null; // same as agency
		} else if (RSN_PURPLE.equalsIgnoreCase(gRoute.getRouteShortName()) || RSN_PRPLE.equalsIgnoreCase(gRoute.getRouteShortName())) {
			return COLOR_7040A4;
		} else if (RSN_RED.equalsIgnoreCase(gRoute.getRouteShortName())) {
			return COLOR_EE1E23;
		}
		throw new MTLog.Fatal("Unexpected route color for %s!", gRoute);
	}

	private static HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;
	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<>();
		ALL_ROUTE_TRIPS2 = map2;
	}

	@Override
	public String cleanStopOriginalId(String gStopId) {
		gStopId = STARTS_WITH_WEGO_NF_A00.matcher(gStopId).replaceAll(StringUtils.EMPTY);
		return gStopId;
	}

	@Override
	public int compareEarly(long routeId, List<MTripStop> list1, List<MTripStop> list2, MTripStop ts1, MTripStop ts2, GStop ts1GStop, GStop ts2GStop) {
		if (ALL_ROUTE_TRIPS2.containsKey(routeId)) {
			return ALL_ROUTE_TRIPS2.get(routeId).compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop, this);
		}
		return super.compareEarly(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
	}

	@Override
	public ArrayList<MTrip> splitTrip(MRoute mRoute, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return ALL_ROUTE_TRIPS2.get(mRoute.getId()).getAllTrips();
		}
		return super.splitTrip(mRoute, gTrip, gtfs);
	}

	@Override
	public Pair<Long[], Integer[]> splitTripStop(MRoute mRoute, GTrip gTrip, GTripStop gTripStop, ArrayList<MTrip> splitTrips, GSpec routeGTFS) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS2.get(mRoute.getId()), this);
		}
		return super.splitTripStop(mRoute, gTrip, gTripStop, splitTrips, routeGTFS);
	}

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return; // split
		}
		mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), gTrip.getDirectionId());
	}

	@Override
	public boolean mergeHeadsign(MTrip mTrip, MTrip mTripToMerge) {
		List<String> headsignsValues = Arrays.asList(mTrip.getHeadsignValue(), mTripToMerge.getHeadsignValue());
		if (mTrip.getRouteId() == RID_RED) {
			if (Arrays.asList( //
					"Garner Rd Expres", // <>
					"Garner Rd" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Garner Rd", mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					"Lundy's Ln", //
					"Clifton Hl - Lundy's Ln" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Clifton Hl - Lundy's Ln", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == RID_BLUE) {
			if (Arrays.asList( //
					"Marineland", //
					"Stanley Av & Convention Ctr", //
					"Convention Ctr" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Convention Ctr", mTrip.getHeadsignId());
				return true;
			}
		}
		throw new MTLog.Fatal("Unexpected trips to merge %s & %s!", mTrip, mTripToMerge);
	}

	private static final Pattern STARTS_WITH_BOUNDS_SLASH = Pattern.compile("(^(.* )?(inbound|outbound)(/))", Pattern.CASE_INSENSITIVE);

	private static final Pattern STARTS_WITH_RSN_ = Pattern.compile("(^[\\d]+( )?)", Pattern.CASE_INSENSITIVE);
	private static final Pattern STARTS_WITH_RLN_DASH = Pattern.compile("(^[^\\-]+\\-)", Pattern.CASE_INSENSITIVE);

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		if (Utils.isUppercaseOnly(tripHeadsign, true, true)) {
			tripHeadsign = tripHeadsign.toLowerCase(Locale.ENGLISH);
		}
		tripHeadsign = STARTS_WITH_RSN_.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = STARTS_WITH_RLN_DASH.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = STARTS_WITH_BOUNDS_SLASH.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = CleanUtils.keepToAndRemoveVia(tripHeadsign);
		tripHeadsign = CleanUtils.cleanNumbers(tripHeadsign);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	@Override
	public String cleanStopName(String gStopName) {
		if (Utils.isUppercaseOnly(gStopName, true, true)) {
			gStopName = gStopName.toLowerCase(Locale.ENGLISH);
		}
		gStopName = CleanUtils.cleanNumbers(gStopName);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}

	private static final String ZERO_0 = "0";

	private static final Pattern STARTS_WITH_WEGO_NF_A00 = Pattern.compile("((^){1}((wego|nf|nft|allnrt)\\_[a-z]{1,3}[\\d]{2,4}(\\_)?)+(stop|sto)?)",
			Pattern.CASE_INSENSITIVE);

	// STOP CODE REQUIRED FOR REAL-TIME API
	@Override
	public String getStopCode(GStop gStop) {
		String stopCode = gStop.getStopCode();
		if (stopCode == null || stopCode.length() == 0 || ZERO_0.equals(stopCode)) {
			stopCode = gStop.getStopId();
		}
		stopCode = STARTS_WITH_WEGO_NF_A00.matcher(stopCode).replaceAll(StringUtils.EMPTY);
		if ("TablRock".equals(stopCode)) {
			return "8871";
		}
		if ("Sta&6039".equalsIgnoreCase(stopCode)) {
			return StringUtils.EMPTY;
		}
		if (StringUtils.isEmpty(stopCode)) {
			throw new MTLog.Fatal("Unexptected stop code for %s!", gStop);
		}
		return stopCode;
	}

	private static final Pattern DIGITS = Pattern.compile("[\\d]+");

	@Override
	public int getStopId(GStop gStop) {
		String stopCode = gStop.getStopCode();
		if (stopCode == null || stopCode.length() == 0 || ZERO_0.equals(stopCode)) {
			stopCode = gStop.getStopId();
		}
		stopCode = STARTS_WITH_WEGO_NF_A00.matcher(stopCode).replaceAll(StringUtils.EMPTY);
		if ("TablRock".equals(stopCode)) {
			return 8871;
		}
		if (Utils.isDigitsOnly(stopCode)) {
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
