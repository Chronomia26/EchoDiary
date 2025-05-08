package com.bigo143.echodiary;
import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.bigo143.echodiary.R; // <-- Change to your actual package

public class BottomSheetDialogHelper {
    public static void showBottomSheet(Context context) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.bottomsheetlayout, null);
        bottomSheetDialog.setContentView(view);

        // Optional: close button logic
        View cancelBtn = view.findViewById(R.id.cancelButton);
        if (cancelBtn != null) {
            cancelBtn.setOnClickListener(v -> bottomSheetDialog.dismiss());
        }

        bottomSheetDialog.show();
    }
}
