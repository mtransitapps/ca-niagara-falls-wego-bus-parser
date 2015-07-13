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

// http://www.niagararegion.ca/government/opendata/data-set.aspx#id=32
// http://maps-dev.niagararegion.ca/GoogleTransit/NiagaraRegionTransit.zip
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
		this.serviceIds = extractUsefulServiceIds(args, this);
		super.start(args);
		System.out.printf("\nGenerating WEGO bus data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
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

	private static final String WEGO = "WEGO";

	@Override
	public boolean excludeRoute(GRoute gRoute) {
		if (!gRoute.agency_id.startsWith(WEGO)) {
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
		Matcher matcher = DIGITS.matcher(gRoute.route_id);
		matcher.find();
		return Long.parseLong(matcher.group());
	}

	@Override
	public String getRouteShortName(GRoute gRoute) {
		return super.getRouteShortName(gRoute); // used by real-time API
	}

	private static final String DOWNTOWN = "Downtown";
	private static final String FALLSVIEW_CLIFTON_HL = "Fallsview / Clifton Hl";
	private static final String LUNDY_S_LN = "Lundy's Ln";
	private static final String NIAGARA_PKS = "Niagara Pks";
	private static final String NOTL_SHUTTLE = "NOTL Shuttle";

	private static final String RNS_BLUE = "Blue";
	private static final String RSN_GREEN = "Green";
	private static final String RSN_ORANGE = "Orange";
	private static final String RSN_PRPLE = "Prple";
	private static final String RSN_PURPLE = "Purple";
	private static final String RSN_RED = "Red";

	@Override
	public String getRouteLongName(GRoute gRoute) {
		if (RNS_BLUE.equals(gRoute.route_short_name)) {
			return FALLSVIEW_CLIFTON_HL;
		} else if (RSN_GREEN.equals(gRoute.route_short_name)) {
			return NIAGARA_PKS;
		} else if (RSN_ORANGE.equals(gRoute.route_short_name)) {
			return NOTL_SHUTTLE;
		} else if (RSN_PURPLE.equals(gRoute.route_short_name) || RSN_PRPLE.equals(gRoute.route_short_name)) {
			return DOWNTOWN;
		} else if (RSN_RED.equals(gRoute.route_short_name)) {
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
		if (RNS_BLUE.equals(gRoute.route_short_name)) {
			return COLOR_5484CC;
		} else if (RSN_GREEN.equals(gRoute.route_short_name)) {
			return COLOR_45BA67;
		} else if (RSN_ORANGE.equals(gRoute.route_short_name)) {
			return null; // same as agency
		} else if (RSN_PURPLE.equals(gRoute.route_short_name) || RSN_PRPLE.equals(gRoute.route_short_name)) {
			return COLOR_7040A4;
		} else if (RSN_RED.equals(gRoute.route_short_name)) {
			return COLOR_EE1E23;
		}
		System.out.printf("\nUnexpected route long name for %s!\n", gRoute);
		System.exit(-1);
		return null;
	}

	private static HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;
	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<Long, RouteTripSpec>();
		map2.put(
				300l,
				new RouteTripSpec(300l, // Green
						MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.NORTH.id, // Queenston
						MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.SOUTH.id) // Rapidsview
						.addTripSort(
								MDirectionType.NORTH.intValue(), //
								Arrays.asList(new String[] { "WEGO_SUM_65", "WEGO_SUM_74", "WEGO_SUM_75", "WEGO_SUM_76", "WEGO_SUM_77", "WEGO_SUM_78",
										"WEGO_SUM_79", "WEGO_SUM_80", "WEGO_SUM_90", "WEGO_SUM_91", "WEGO_SUM_92", "WEGO_SUM_93", "WEGO_SUM_286" })) //
						.addTripSort(
								MDirectionType.SOUTH.intValue(), //
								Arrays.asList(new String[] { "WEGO_SUM_93", "WEGO_SUM_92", "WEGO_SUM_94", "WEGO_SUM_91", "WEGO_SUM_95", "WEGO_SUM_80",
										"WEGO_SUM_140", "WEGO_SUM_81", "WEGO_SUM_86", "WEGO_SUM_34", "WEGO_SUM_96", "WEGO_SUM_65" })) //
						.compileBothTripSort());
		map2.put(400l, new RouteTripSpec(400l, // Blue
				MInboundType.INBOUND.intValue(), MTrip.HEADSIGN_TYPE_INBOUND, MInboundType.INBOUND.id, // Table Rock
				MInboundType.OUTBOUND.intValue(), MTrip.HEADSIGN_TYPE_INBOUND, MInboundType.OUTBOUND.id) // Convention Ctr
				.addTripSort(MInboundType.INBOUND.intValue(), //
						Arrays.asList(new String[] { "WEGO_SUM_248", "WEGO_SUM_67", "WEGO_SUM_252", "WEGO_SUM_245", "WEGO_SUM_34" })) //
				.addTripSort(MInboundType.OUTBOUND.intValue(), //
						Arrays.asList(new String[] { "WEGO_SUM_34", "WEGO_SUM_58", "WEGO_SUM_290", "WEGO_SUM_67", "WEGO_SUM_248" })) //
				.compileBothTripSort());
		map2.put(500l, new RouteTripSpec(500l, // Red
				MInboundType.INBOUND.intValue(), MTrip.HEADSIGN_TYPE_INBOUND, MInboundType.INBOUND.id, // Table Rock
				MInboundType.OUTBOUND.intValue(), MTrip.HEADSIGN_TYPE_INBOUND, MInboundType.OUTBOUND.id) // Lundy's Lane
				.addTripSort(MInboundType.INBOUND.intValue(), //
						Arrays.asList(new String[] { "WEGO_SUM_20", "WEGO_SUM_27", "WEGO_SUM_34" })) //
				.addTripSort(MInboundType.OUTBOUND.intValue(), //
						Arrays.asList(new String[] { "WEGO_SUM_34", "WEGO_SUM_282", "WEGO_SUM_37", "WEGO_SUM_273", "WEGO_SUM_20" })) //
				.compileBothTripSort());
		map2.put(600l, new RouteTripSpec(600l, // Purple
				MInboundType.INBOUND.intValue(), MTrip.HEADSIGN_TYPE_INBOUND, MInboundType.INBOUND.id, // Table Rock
				MInboundType.OUTBOUND.intValue(), MTrip.HEADSIGN_TYPE_INBOUND, MInboundType.OUTBOUND.id) // Downtown
				.addTripSort(MInboundType.INBOUND.intValue(), //
						Arrays.asList(new String[] { "WEGO_SUM_9", "WEGO_SUM_143", "WEGO_SUM_34" })) //
				.addTripSort(MInboundType.OUTBOUND.intValue(), //
						Arrays.asList(new String[] { "WEGO_SUM_34", "WEGO_SUM_3", "WEGO_SUM_4", "WEGO_SUM_9" })) //
				.compileBothTripSort());
		ALL_ROUTE_TRIPS2 = map2;
	}

	@Override
	public int compareEarly(long routeId, List<MTripStop> list1, List<MTripStop> list2, MTripStop ts1, MTripStop ts2, GStop ts1GStop, GStop ts2GStop) {
		if (ALL_ROUTE_TRIPS2.containsKey(routeId)) {
			return ALL_ROUTE_TRIPS2.get(routeId).compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
		}
		System.out.printf("\n%s: Unexpected compare early route!\n", routeId);
		System.exit(-1);
		return -1;
	}

	@Override
	public ArrayList<MTrip> splitTrip(MRoute mRoute, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.id)) {
			return ALL_ROUTE_TRIPS2.get(mRoute.id).getAllTrips();
		}
		System.out.printf("\n%s: Unexpected split trip route!\n", mRoute.id);
		System.exit(-1);
		return null;
	}

	@Override
	public Pair<Long[], Integer[]> splitTripStop(MRoute mRoute, GTrip gTrip, GTripStop gTripStop, ArrayList<MTrip> splitTrips, GSpec routeGTFS) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.id)) {
			RouteTripSpec rts = ALL_ROUTE_TRIPS2.get(mRoute.id);
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, //
					rts.getBeforeAfterStopIds(0), //
					rts.getBeforeAfterStopIds(1), //
					rts.getBeforeAfterBothStopIds(0), //
					rts.getBeforeAfterBothStopIds(1), //
					rts.getTripId(0), //
					rts.getTripId(1), //
					rts.getAllBeforeAfterStopIds());
		}
		System.out.printf("\n%s: Unexptected split trip stop route!\n", mRoute.id);
		System.exit(-1);
		return null;
	}

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.id)) {
			return; // split
		}
		System.out.printf("\n%s: Unexpected trip %s!\n", mRoute.id, gTrip);
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
		String stopCode = STOP_CODES.get(gStop.stop_id);
		if (stopCode != null) {
			return stopCode;
		}
		return super.getStopCode(gStop);
	}

	private static final Pattern DIGITS = Pattern.compile("[\\d]+");

	@Override
	public int getStopId(GStop gStop) {
		if (Utils.isDigitsOnly(gStop.stop_code)) {
			return Integer.parseInt(gStop.stop_code); // use stop code as stop ID
		}
		Matcher matcher = DIGITS.matcher(gStop.stop_id);
		matcher.find();
		return 100000 + Integer.parseInt(matcher.group());
	}
}
