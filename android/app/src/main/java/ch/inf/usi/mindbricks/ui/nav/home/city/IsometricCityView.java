package ch.inf.usi.mindbricks.ui.nav.home.city;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import ch.inf.usi.mindbricks.util.PreferencesManager;

public class IsometricCityView extends View {

    private List<CitySlot> slots = new ArrayList<>();
    private final Paint paintUnlocked = new Paint();
    private final Paint paintLocked = new Paint();
    private final Paint paintOutline = new Paint();
    private final Paint paintBuilding = new Paint();

    private float cellWidth;
    private float cellHeight;

    public IsometricCityView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paintUnlocked.setColor(Color.GREEN);
        paintLocked.setColor(Color.RED);
        paintOutline.setColor(Color.BLACK);
        paintOutline.setStyle(Paint.Style.STROKE);
        paintOutline.setStrokeWidth(4f);
        paintBuilding.setColor(Color.BLUE);
    }

    public void setSlots(List<CitySlot> slots) {
        this.slots = slots;
        invalidate(); // redraw the view
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (slots == null || slots.isEmpty()) return;

        int maxCol = 0, maxRow = 0;
        for (CitySlot slot : slots) {
            if (slot.getCol() > maxCol) maxCol = slot.getCol();
            if (slot.getRow() > maxRow) maxRow = slot.getRow();
        }

        cellWidth = getWidth() / (float) (maxCol + 1);
        cellHeight = getHeight() / (float) (maxRow + 1);

        for (CitySlot slot : slots) {
            float left = slot.getCol() * cellWidth;
            float top = slot.getRow() * cellHeight;
            float right = left + cellWidth;
            float bottom = top + cellHeight;

            // Fill slot
            canvas.drawRect(left, top, right, bottom, slot.isUnlocked() ? paintUnlocked : paintLocked);
            // Outline
            canvas.drawRect(left, top, right, bottom, paintOutline);

            // Draw building if assigned
            if (slot.getBuilding() != null) {
                float cx = left + cellWidth / 2f;
                float cy = top + cellHeight / 2f;
                float radius = Math.min(cellWidth, cellHeight) / 4f;
                canvas.drawCircle(cx, cy, radius, paintBuilding);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float touchX = event.getX();
            float touchY = event.getY();

            CitySlot clickedSlot = findClickedSlot(touchX, touchY);
            if (clickedSlot != null && clickedSlot.isUnlocked()) {
                showBuildingSelectionDialog(clickedSlot);
            }
        }
        return true;
    }

    private CitySlot findClickedSlot(float x, float y) {
        for (CitySlot slot : slots) {
            float left = slot.getCol() * cellWidth;
            float top = slot.getRow() * cellHeight;
            float right = left + cellWidth;
            float bottom = top + cellHeight;

            if (x >= left && x <= right && y >= top && y <= bottom) {
                return slot;
            }
        }
        return null;
    }

    private void showBuildingSelectionDialog(CitySlot slot) {
        Context context = getContext();
        PreferencesManager prefs = new PreferencesManager(context);
        String[] purchasedBuildings = prefs.getPurchasedItemIds().toArray(new String[0]);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Select Building");
        builder.setItems(purchasedBuildings, (dialog, which) -> {
            slot.setBuilding(purchasedBuildings[which]);
            invalidate();
        });

        if (slot.getBuilding() != null) {
            builder.setNeutralButton("Remove Building", (dialog, which) -> {
                slot.setBuilding(null);
                invalidate();
            });
        }

        builder.show();
    }
}
