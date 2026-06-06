package com.github.meypod.al_azan.core.util.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * Verifies {@link TSAGeoMag} against the official WMM2025 test values published by NOAA NCEI
 * ({@code WMM2025_TestValues.txt}, bundled under {@code src/test/resources}).
 *
 * <p>The model is data-driven: each line of the test file lists a (year, altitude, lat, long)
 * sample plus the expected declination, inclination, and the H/X/Y/Z/F field components. To update
 * for a future WMM epoch, drop the new coefficients into {@link TSAGeoMag} and the new test-values
 * file here — no assertions to hand-edit.
 *
 * <p>Columns (1-indexed) in the test file:
 * <pre>
 *  1 decimal year   2 altitude(km)   3 lat(deg)   4 long(deg)
 *  5 declination    6 inclination    7 H   8 X(north)   9 Y(east)   10 Z(vertical)   11 F
 * </pre>
 */
public class TSAGeoMagTest {

    private static final String TEST_VALUES_FILE = "WMM2025_TestValues.txt";

    /** Declination/inclination are published to 2 decimals; allow half-ULP rounding plus margin. */
    private static final double ANGLE_TOLERANCE = 1.0E-2;
    /** Field components are published at full precision; nT agreement is well under this. */
    private static final double INTENSITY_TOLERANCE = 5.0E-2;

    @Test
    public void matchesOfficialWmm2025TestValues() throws Exception {
        List<double[]> rows = loadTestValues();
        assertTrue("Expected test value rows to be loaded", rows.size() > 50);

        TSAGeoMag magModel = new TSAGeoMag();
        List<String> failures = new ArrayList<>();

        for (double[] r : rows) {
            double year = r[0];
            double alt = r[1];
            double lat = r[2];
            double lon = r[3];
            double expDec = r[4];
            double expInc = r[5];
            double expH = r[6];
            double expX = r[7];
            double expY = r[8];
            double expZ = r[9];
            double expF = r[10];

            checkAngle(failures, "declination", lat, lon, year, alt, expDec,
                    magModel.getDeclination(lat, lon, year, alt));
            checkAngle(failures, "inclination", lat, lon, year, alt, expInc,
                    magModel.getDipAngle(lat, lon, year, alt));
            check(failures, "H", lat, lon, year, alt, expH,
                    magModel.getHorizontalIntensity(lat, lon, year, alt), INTENSITY_TOLERANCE);
            check(failures, "X", lat, lon, year, alt, expX,
                    magModel.getNorthIntensity(lat, lon, year, alt), INTENSITY_TOLERANCE);
            check(failures, "Y", lat, lon, year, alt, expY,
                    magModel.getEastIntensity(lat, lon, year, alt), INTENSITY_TOLERANCE);
            check(failures, "Z", lat, lon, year, alt, expZ,
                    magModel.getVerticalIntensity(lat, lon, year, alt), INTENSITY_TOLERANCE);
            check(failures, "F", lat, lon, year, alt, expF,
                    magModel.getIntensity(lat, lon, year, alt), INTENSITY_TOLERANCE);
        }

        if (!failures.isEmpty()) {
            fail(failures.size() + " WMM2025 mismatch(es):\n" + String.join("\n", failures));
        }
    }

    private void check(List<String> failures, String field, double lat, double lon, double year,
                       double alt, double expected, double actual, double tolerance) {
        if (Math.abs(expected - actual) > tolerance) {
            failures.add(describe(field, lat, lon, year, alt, expected, actual));
        }
    }

    /** Declination/inclination compared modulo 360 so a value near ±180 doesn't false-fail. */
    private void checkAngle(List<String> failures, String field, double lat, double lon, double year,
                            double alt, double expected, double actual) {
        double diff = ((expected - actual + 540) % 360) - 180;
        if (Math.abs(diff) > ANGLE_TOLERANCE) {
            failures.add(describe(field, lat, lon, year, alt, expected, actual));
        }
    }

    private String describe(String field, double lat, double lon, double year, double alt,
                            double expected, double actual) {
        return String.format(
                "%s @ year=%.1f alt=%.0f lat=%.0f lon=%.0f: expected %.5f but was %.5f",
                field, year, alt, lat, lon, expected, actual);
    }

    private List<double[]> loadTestValues() throws Exception {
        List<double[]> rows = new ArrayList<>();
        InputStream in = getClass().getClassLoader().getResourceAsStream(TEST_VALUES_FILE);
        assertNotNull("Missing test resource " + TEST_VALUES_FILE, in);
        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split("\\s+");
                if (parts.length < 11) {
                    continue;
                }
                double[] row = new double[11];
                for (int i = 0; i < 11; i++) {
                    row[i] = Double.parseDouble(parts[i]);
                }
                rows.add(row);
            }
        }
        return rows;
    }

    /**
     * Outside the model's 5-year validity window the date is clamped to the nearest boundary
     * instead of extrapolating unboundedly: any date past {@code epoch + 5} (2030 for WMM2025)
     * yields the field as modeled at the window end, and any date before {@code epoch} yields the
     * field at the epoch. This keeps a stale build's error bounded until newer coefficients ship.
     */
    @Test
    public void clampsDatesOutsideValidityWindow() {
        TSAGeoMag mag = new TSAGeoMag();
        double lat = 35.0;
        double lon = 51.0;
        double alt = 0.0;

        double atWindowEnd = mag.getDeclination(lat, lon, 2030.0, alt);
        assertEquals("just past window == window end",
                atWindowEnd, mag.getDeclination(lat, lon, 2030.5, alt), 0.0);
        assertEquals("far past window == window end",
                atWindowEnd, mag.getDeclination(lat, lon, 2099.0, alt), 0.0);

        double atEpoch = mag.getDeclination(lat, lon, 2025.0, alt);
        assertEquals("before epoch == epoch",
                atEpoch, mag.getDeclination(lat, lon, 2000.0, alt), 0.0);

        // Sanity: within the window the date still matters (not clamped flat).
        assertNotEquals("within window should vary with date",
                atEpoch, atWindowEnd, 1.0E-6);
    }

    private static final String COF_RESOURCE =
            "com/github/meypod/al_azan/core/util/android/WMM.COF";

    /**
     * Guards the embedded coefficient fallback against drift: the {@code input[]} array compiled
     * into {@link TSAGeoMag} (used when the bundled {@code WMM.COF} resource can't be loaded) must
     * carry the exact same epoch and coefficients as that resource. Updating one without the other
     * would silently serve stale field data on the fallback path — which the official-values test
     * above never exercises, since the resource is present on the test classpath.
     */
    @Test
    public void embeddedCoefficientsMatchBundledCofFile() throws Exception {
        Model fromFile = parseModel(readCofResourceLines());
        Model fromEmbedded = parseModel(readEmbeddedInputArray());

        assertEquals("epoch", fromFile.epoch, fromEmbedded.epoch, 0.0);
        assertEquals(
                "coefficient row count",
                fromFile.rows.size(),
                fromEmbedded.rows.size());
        for (int i = 0; i < fromFile.rows.size(); i++) {
            double[] expected = fromFile.rows.get(i);
            double[] actual = fromEmbedded.rows.get(i);
            for (int c = 0; c < expected.length; c++) {
                assertEquals(
                        "row " + i + " col " + c,
                        expected[c],
                        actual[c],
                        0.0);
            }
        }
    }

    private static final class Model {
        final double epoch;
        final List<double[]> rows;

        Model(double epoch, List<double[]> rows) {
            this.epoch = epoch;
            this.rows = rows;
        }
    }

    /** Parses WMM-format lines: header (epoch first) then "n m g h dg dh" rows until a 9999 fence. */
    private Model parseModel(List<String> lines) {
        double epoch = Double.parseDouble(lines.get(0).trim().split("\\s+")[0]);
        List<double[]> rows = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String[] parts = lines.get(i).trim().split("\\s+");
            if (parts.length < 6) {
                continue;
            }
            if (Double.parseDouble(parts[0]) >= 9999) {
                break;
            }
            double[] row = new double[6];
            for (int c = 0; c < 6; c++) {
                row[c] = Double.parseDouble(parts[c]);
            }
            rows.add(row);
        }
        return new Model(epoch, rows);
    }

    private List<String> readCofResourceLines() throws Exception {
        InputStream in = getClass().getClassLoader().getResourceAsStream(COF_RESOURCE);
        assertNotNull("Missing bundled resource " + COF_RESOURCE, in);
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    lines.add(line);
                }
            }
        }
        return lines;
    }

    private List<String> readEmbeddedInputArray() throws Exception {
        Field field = TSAGeoMag.class.getDeclaredField("input");
        field.setAccessible(true);
        String[] embedded = (String[]) field.get(new TSAGeoMag());
        List<String> lines = new ArrayList<>();
        for (String line : embedded) {
            if (line != null && !line.trim().isEmpty()) {
                lines.add(line);
            }
        }
        return lines;
    }
}
