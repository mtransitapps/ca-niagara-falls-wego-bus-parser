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
		System.out.printf("\nGenerating WEGO bus data...");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this, true);
		super.start(args);
		System.out.printf("\nGenerating WEGO bus data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
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
				&& !gRoute.getAgencyId().contains("Niagara Falls Transit & WeGo")) {
			return true;
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
		routeId = STARTS_WITH_WEGO_NF_A00.matcher(routeId).replaceAll(StringUtils.EMPTY);
		Matcher matcher = DIGITS.matcher(routeId);
		if (matcher.find()) {
			return Long.parseLong(matcher.group());
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
		System.out.printf("\nUnexpected route ID for %s!\n", gRoute);
		System.exit(-1);
		return -1l;
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
				System.out.printf("\nUnexpected route short name for %s!\n", gRoute);
				System.exit(-1);
				return null;
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
				System.out.printf("\nUnexpected route long name for %s!\n", gRoute);
				System.exit(-1);
				return null;
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
		System.out.printf("\nUnexpected route long name for %s!\n", gRoute);
		System.exit(-1);
		return null;
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
				System.out.printf("\nUnexpected route color for %s!\n", gRoute);
				System.exit(-1);
				return null;
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
		System.out.printf("\nUnexpected route color for %s!\n", gRoute);
		System.exit(-1);
		return null;
	}

	private static HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;
	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<Long, RouteTripSpec>();
		map2.put(RID_GREEN, new RouteTripSpec(RID_GREEN, // Green
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.NORTH.getId(), // Queenston
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.SOUTH.getId()) // Rapidsview
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"13131", // RAPIDSVIEW"
								"8006", // !=
								"13031", // <> AERO CAR NORTH
								"14868", // != AERIAL ADVENTURE
								"13038", // != BUTFLY Turnaround =>
								"8020", // != Niagara Glen
								"13070", // != FLORAL CLOCK =>
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"13070", // FLORAL CLOCK <=
								"13040", // BUTTERFLY CONS
								"13038", // BUTFLY Turnaround <=
								"8033", // Whirlpool Golf S
								"13032", // AERO CAR SOUTH
								"13031", // != <> AERO CAR NORTH <=
								"8035", // == Souvenir City
								"13131", // RAPIDSVIEW
						})) //
				.compileBothTripSort());
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
		System.out.printf("\nUnexpected trips to merge %s & %s!\n", mTrip, mTripToMerge);
		System.exit(-1);
		return false;
	}

	private static final Pattern STARTS_WITH_TO = Pattern.compile("(^(.* )?to )", Pattern.CASE_INSENSITIVE);

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		if (Utils.isUppercaseOnly(tripHeadsign, true, true)) {
			tripHeadsign = tripHeadsign.toLowerCase(Locale.ENGLISH);
		}
		tripHeadsign = STARTS_WITH_TO.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
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

	private static final Pattern STARTS_WITH_WEGO_NF_A00 = Pattern.compile("((^){1}((wego||nf)\\_[A-Z]{1,3}[\\d]{2}(\\_)?)+(stop|sto)?)",
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
			System.out.printf("\nUnexptected stop code for %s!\n", gStop);
			System.exit(-1);
			return null;
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
		System.out.printf("\nUnexpected stop ID %s!\n", gStop);
		System.exit(-1);
		return -1;
	}
}
