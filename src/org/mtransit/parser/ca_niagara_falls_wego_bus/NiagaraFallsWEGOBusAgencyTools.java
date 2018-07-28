package org.mtransit.parser.ca_niagara_falls_wego_bus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.mtransit.parser.mt.data.MInboundType;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTrip;
import org.mtransit.parser.mt.data.MTripStop;

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

	private static final String WEGO = "WeGo";

	@Override
	public boolean excludeRoute(GRoute gRoute) {
		if (!gRoute.getAgencyId().contains(WEGO)) {
			return true;
		}
		return super.excludeRoute(gRoute);
	}

	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	@Override
	public long getRouteId(GRoute gRoute) {
		Matcher matcher = DIGITS.matcher(gRoute.getRouteId());
		if (matcher.find()) {
			return Long.parseLong(matcher.group());
		}
		if (RSN_RED.equalsIgnoreCase(gRoute.getRouteShortName())) {
			return 300L;
		} else if (RSN_BLUE.equalsIgnoreCase(gRoute.getRouteShortName())) {
			return 301L;
		} else if (RSN_GREEN.equalsIgnoreCase(gRoute.getRouteShortName())) {
			return 301L;
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
			case 301: return RSN_BLUE; // Blue
			case 302: return RSN_GREEN; // Green
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

	private static final String RSN_BLUE = "Blue";
	private static final String RSN_GREEN = "Green";
	private static final String RSN_ORANGE = "Orange";
	private static final String RSN_PRPLE = "Prple";
	private static final String RSN_PURPLE = "Purple";
	private static final String RSN_RED = "Red";

	@Override
	public String getRouteLongName(GRoute gRoute) {
		if (Utils.isDigitsOnly(gRoute.getRouteShortName())) {
			int rsn = Integer.parseInt(gRoute.getRouteShortName());
			switch (rsn) {
			// @formatter:off
			case 300: return LUNDY_S_LN; // Red
			case 301: return FALLSVIEW_CLIFTON_HL; // Blue
			case 302: return NIAGARA_PKS; // Green
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
			case 300: return COLOR_EE1E23; // Red
			case 301: return COLOR_5484CC; // Blue
			case 302: return COLOR_45BA67; // Green
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
		map2.put(300L, new RouteTripSpec(300L, // Red
				MInboundType.INBOUND.intValue(), MTrip.HEADSIGN_TYPE_INBOUND, MInboundType.INBOUND.getId(), // Table Rock
				MInboundType.OUTBOUND.intValue(), MTrip.HEADSIGN_TYPE_INBOUND, MInboundType.OUTBOUND.getId()) // Lundy's Lane
				.addTripSort(MInboundType.INBOUND.intValue(), //
						Arrays.asList(new String[] { //
						"Stop8882", // Lundy's Lane & Campark Turnaroun
								"Stop8684", //
								"Stop8014", //
								"Stop8871" // Table Rock
						})) //
				.addTripSort(MInboundType.OUTBOUND.intValue(), //
						Arrays.asList(new String[] { //
						"Stop8871", // Table Rock
								"Stop8002", //
								"Stop8622", // Stanley Av & DoubleTree Hotel
								"Stop8881", //
								"Stop8882" // Lundy's Lane & Campark Turnaroun
						})) //
				.compileBothTripSort());
		map2.put(301L, new RouteTripSpec(301L, // Blue
				MInboundType.INBOUND.intValue(), MTrip.HEADSIGN_TYPE_INBOUND, MInboundType.INBOUND.getId(), // Table Rock
				MInboundType.OUTBOUND.intValue(), MTrip.HEADSIGN_TYPE_INBOUND, MInboundType.OUTBOUND.getId()) // Convention Ctr
				.addTripSort(MInboundType.INBOUND.intValue(), //
						Arrays.asList(new String[] { //
						"Stop8950", // != Convention Centre <=
								"StopSCT1", // != Stanley Av & 6815 <=
								"StopSCT2", // !=
								"Stop8873", // ==
								"Stop8871" // Table Rock
						})) //
				.addTripSort(MInboundType.OUTBOUND.intValue(), //
						Arrays.asList(new String[] { //
						"Stop8871", // Table Rock
								"Stop8CD1", // ==
								"StopSCT1", // != Stanley Av & 6815 =>
								"Stop8950" // != Convention Centre =>
						})) //
				.compileBothTripSort());
		map2.put(302L, new RouteTripSpec(302L, // Green
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.NORTH.getId(), // Queenston
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.SOUTH.getId()) // Rapidsview
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"Stop8036", // Rapidsview North
								"Stop8004", //
								"Stop8021" // Butterfly Conservatory
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"Stop8021", // Butterfly Conservatory
								"Stop8009", //
								"Stop8036" // Rapidsview North
						})) //
				.compileBothTripSort());
		ALL_ROUTE_TRIPS2 = map2;
	}

	@Override
	public int compareEarly(long routeId, List<MTripStop> list1, List<MTripStop> list2, MTripStop ts1, MTripStop ts2, GStop ts1GStop, GStop ts2GStop) {
		if (ALL_ROUTE_TRIPS2.containsKey(routeId)) {
			return ALL_ROUTE_TRIPS2.get(routeId).compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop, this);
		}
		System.out.printf("\n%s: Unexpected compare early route!\n", routeId);
		System.exit(-1);
		return -1;
	}

	@Override
	public ArrayList<MTrip> splitTrip(MRoute mRoute, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return ALL_ROUTE_TRIPS2.get(mRoute.getId()).getAllTrips();
		}
		System.out.printf("\n%s: Unexpected split trip route!\n", mRoute.getId());
		System.exit(-1);
		return null;
	}

	@Override
	public Pair<Long[], Integer[]> splitTripStop(MRoute mRoute, GTrip gTrip, GTripStop gTripStop, ArrayList<MTrip> splitTrips, GSpec routeGTFS) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS2.get(mRoute.getId()), this);
		}
		System.out.printf("\n%s: Unexptected split trip stop route!\n", mRoute.getId());
		System.exit(-1);
		return null;
	}

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return; // split
		}
		System.out.printf("\n%s: Unexpected trip %s!\n", mRoute.getId(), gTrip);
		System.exit(-1);
	}

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		tripHeadsign = tripHeadsign.toLowerCase(Locale.ENGLISH);
		tripHeadsign = CleanUtils.cleanNumbers(tripHeadsign);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	@Override
	public String cleanStopName(String gStopName) {
		gStopName = CleanUtils.cleanNumbers(gStopName);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}

	private static final HashMap<String, String> STOP_CODES;
	static {
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("WEGO_SUM_284", "CD2"); // Lundy's Lane + Brookfield (North Side)
		map.put("WEGO_SUM_290", ""); // Lundy's Lane + Brookfield (North Side)
		STOP_CODES = map;
	}

	@Override
	public String getStopCode(GStop gStop) {
		String stopCode = STOP_CODES.get(gStop.getStopId());
		if (stopCode != null) {
			return stopCode;
		}
		if ("0".equals(gStop.getStopCode())) {
			return null;
		}
		return super.getStopCode(gStop);
	}

	private static final Pattern DIGITS = Pattern.compile("[\\d]+");

	@Override
	public int getStopId(GStop gStop) {
		Matcher matcher = DIGITS.matcher(gStop.getStopId());
		if (matcher.find()) {
			return Integer.parseInt(matcher.group());
		}
		if ("MAR".equalsIgnoreCase(gStop.getStopId())) {
			return 900000;
		}
		System.out.printf("\nUnexpected stop ID %s!\n", gStop);
		System.exit(-1);
		return -1;
	}
}
