package com.eveningoutpost.dexdrip.models;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.insulin.Insulin;
import com.eveningoutpost.dexdrip.insulin.InsulinManager;

import org.junit.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static com.eveningoutpost.dexdrip.utilitymodels.Constants.DAY_IN_MS;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.MONTH_IN_MS;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import lombok.val;

/**
 * Tests for {@link Treatments}
 *
 * @author Asbjørn Aarrestad - 2019.07 - asbjorn@aarrestad.com
 */
public class TreatmentsTest extends RobolectricTestWithConfig {

    @Test
    public void createAndReadTreatment() {
        // :: Create
        long time = Instant.now().getEpochSecond();
        Treatments.create(55, 2, time);

        // :: Read
        Treatments lastTreatment = Treatments.last();

        // :: Verify
        assertThat(lastTreatment.carbs).isEqualTo(55.0);
        assertThat(lastTreatment.insulin).isEqualTo(2.0);
        assertThat(lastTreatment.timestamp).isEqualTo(time);
        assertThat(lastTreatment.enteredBy).startsWith(Treatments.XDRIP_TAG);
    }


    @Test
    public void createManyReadLast() {
        // :: Setup
        long time = Instant.now().getEpochSecond();
        Treatments.create(1, 8, time);
        Treatments.create(2, 7, time);
        Treatments.create(3, 6, time);
        Treatments.create(4, 5, time);
        Treatments.create(5, 4, time);
        Treatments.create(6, 3, time);
        Treatments.create(7, 2, time);
        Treatments.create(8, 1, time);

        // :: Act
        Treatments lastTreatment = Treatments.last();

        // :: Verify
        assertThat(lastTreatment.carbs).isEqualTo(8.0);
        assertThat(lastTreatment.insulin).isEqualTo(1.0);
        assertThat(lastTreatment.timestamp).isEqualTo(time);
        assertThat(lastTreatment.enteredBy).startsWith(Treatments.XDRIP_TAG);
    }

    @Test
    public void getLastestNoneXdrip_noneEntered() {
        // :: Create
        long time = Instant.now().getEpochSecond();
        Treatments.create(1, 8, time);
        Treatments.create(2, 7, time);
        Treatments.create(3, 6, time);
        Treatments.create(4, 5, time);
        Treatments.create(5, 4, time);
        Treatments.create(6, 3, time);
        Treatments.create(7, 2, time);
        Treatments.create(8, 1, time);

        // :: Read
        Treatments lastTreatment = Treatments.lastNotFromXdrip();

        // :: Verify
        assertThat(lastTreatment).isNull();
    }

    @Test
    public void getLastestNoneXdrip_oneEntered() {
        // :: Create
        long time = Instant.now().getEpochSecond();
        Treatments.create(1, 8, time);
        Treatments.create(2, 7, time);
        Treatments.create(3, 6, time);
        Treatments.create(4, 5, time);
        Treatments.create(5, 4, time);
        Treatments.create(6, 3, time);

        Treatments notFromXdrip = Treatments.create(7, 2, time);
        notFromXdrip.enteredBy = "SomeOtherSource";
        notFromXdrip.save();

        Treatments.create(8, 1, time);

        // :: Read
        Treatments lastTreatment = Treatments.lastNotFromXdrip();

        // :: Verify
        assertThat(lastTreatment.carbs).isEqualTo(7.0);
        assertThat(lastTreatment.insulinFastAmount).isEqualTo(2.0);
        assertThat(lastTreatment.timestamp).isEqualTo(time);
        assertThat(lastTreatment.enteredBy).startsWith("SomeOtherSource");
    }

    @Test
    public void cleanupTest() {
        long ts = JoH.tsl();
        Treatments.delete_all();
        for (long offset = 0; offset < MONTH_IN_MS; offset += DAY_IN_MS) {
            Treatments.createForTest(ts - offset, 1.0);
        }
        int before = Treatments.latestForGraph(1000, 0, ts + DAY_IN_MS).size();
        Treatments.cleanup(5);
        int after = Treatments.latestForGraph(1000, 0, ts + DAY_IN_MS).size();
        assertWithMessage("test before").that(before).isEqualTo(30);
        assertWithMessage("test after").that(after).isEqualTo(5);
        Treatments.delete_all();
    }
}
