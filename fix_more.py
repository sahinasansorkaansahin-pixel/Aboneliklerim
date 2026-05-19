import os
import re

print("Starting fix_more.py")

# 1. Update fragment_more.xml
fragment_file = r"c:\Aboneliklerim\app\src\main\res\layout\fragment_more.xml"
with open(fragment_file, 'r', encoding='utf-8') as f:
    fragment_content = f.read()

# Remove Safe DPI section
dpi_pattern = re.compile(r"<!-- Safe DPI / UI Scale -->.*?<View android:layout_width=\"match_parent\" android:layout_height=\"1dp\" android:background=\"#1AFFFFFF\" android:layout_marginVertical=\"12dp\"/>\s*", re.DOTALL)
fragment_content = dpi_pattern.sub("", fragment_content)

# Add Legal Layout and Overlay Layout
legal_layout = """
            <!-- Legal & Privacy -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/legal_and_privacy"
                android:textColor="@color/text_main"
                android:textSize="14sp"
                android:textStyle="bold"
                android:layout_marginStart="8dp"
                android:layout_marginTop="24dp"
                android:layout_marginBottom="8dp" />

            <androidx.cardview.widget.CardView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginBottom="24dp"
                app:cardBackgroundColor="@color/surface_main"
                app:cardCornerRadius="20dp"
                app:cardElevation="0dp">

                <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
                    android:orientation="vertical" android:padding="8dp">

                    <LinearLayout android:id="@+id/rowPrivacyPolicy" android:layout_width="match_parent" android:layout_height="56dp"
                        android:orientation="horizontal" android:gravity="center_vertical"
                        android:paddingHorizontal="16dp"
                        android:clickable="true" android:focusable="true" android:background="?attr/selectableItemBackground">
                        <ImageView android:layout_width="24dp" android:layout_height="24dp" android:src="@drawable/ic_help_purple" app:tint="#730692"/>
                        <TextView android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:layout_marginStart="12dp"
                            android:text="@string/policy_privacy_title" android:textColor="#730692" android:textSize="16sp" android:textStyle="bold"/>
                        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="›" android:textColor="#730692" android:textSize="24sp"/>
                    </LinearLayout>

                    <View android:layout_width="match_parent" android:layout_height="1dp" android:background="#1AFFFFFF" android:layout_marginHorizontal="16dp"/>

                    <LinearLayout android:id="@+id/rowAccountDeletion" android:layout_width="match_parent" android:layout_height="56dp"
                        android:orientation="horizontal" android:gravity="center_vertical"
                        android:paddingHorizontal="16dp"
                        android:clickable="true" android:focusable="true" android:background="?attr/selectableItemBackground">
                        <ImageView android:layout_width="24dp" android:layout_height="24dp" android:src="@drawable/ic_delete" app:tint="#730692"/>
                        <TextView android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:layout_marginStart="12dp"
                            android:text="@string/policy_deletion_title" android:textColor="#730692" android:textSize="16sp" android:textStyle="bold"/>
                        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="›" android:textColor="#730692" android:textSize="24sp"/>
                    </LinearLayout>

                    <View android:layout_width="match_parent" android:layout_height="1dp" android:background="#1AFFFFFF" android:layout_marginHorizontal="16dp"/>

                    <LinearLayout android:id="@+id/rowCopyright" android:layout_width="match_parent" android:layout_height="56dp"
                        android:orientation="horizontal" android:gravity="center_vertical"
                        android:paddingHorizontal="16dp"
                        android:clickable="true" android:focusable="true" android:background="?attr/selectableItemBackground">
                        <ImageView android:layout_width="24dp" android:layout_height="24dp" android:src="@drawable/ic_category" app:tint="#730692"/>
                        <TextView android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:layout_marginStart="12dp"
                            android:text="@string/policy_copyright_title" android:textColor="#730692" android:textSize="16sp" android:textStyle="bold"/>
                        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="›" android:textColor="#730692" android:textSize="24sp"/>
                    </LinearLayout>

                    <View android:layout_width="match_parent" android:layout_height="1dp" android:background="#1AFFFFFF" android:layout_marginHorizontal="16dp"/>

                    <LinearLayout android:id="@+id/rowAdPartners" android:layout_width="match_parent" android:layout_height="56dp"
                        android:orientation="horizontal" android:gravity="center_vertical"
                        android:paddingHorizontal="16dp"
                        android:clickable="true" android:focusable="true" android:background="?attr/selectableItemBackground">
                        <ImageView android:layout_width="24dp" android:layout_height="24dp" android:src="@drawable/ic_chart" app:tint="#730692"/>
                        <TextView android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:layout_marginStart="12dp"
                            android:text="@string/policy_adpartners_title" android:textColor="#730692" android:textSize="16sp" android:textStyle="bold"/>
                        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="›" android:textColor="#730692" android:textSize="24sp"/>
                    </LinearLayout>

                </LinearLayout>
            </androidx.cardview.widget.CardView>
"""

# Insert Legal Layout right before </LinearLayout> inside the ScrollView
fragment_content = fragment_content.replace(
    """        </LinearLayout>
    </androidx.core.widget.NestedScrollView>""",
    legal_layout + """        </LinearLayout>
    </androidx.core.widget.NestedScrollView>"""
)

overlay_layout = """
    <!-- Custom Fullscreen Overlay for Policies -->
    <FrameLayout
        android:id="@+id/layoutPolicyOverlay"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="#F20B0B14"
        android:clickable="true"
        android:focusable="true"
        android:visibility="gone"
        android:elevation="20dp"
        android:padding="24dp">

        <androidx.cardview.widget.CardView
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:layout_marginVertical="32dp"
            app:cardBackgroundColor="#1A1A24"
            app:cardCornerRadius="24dp"
            app:cardElevation="12dp">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:orientation="vertical">

                <RelativeLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:padding="20dp"
                    android:background="#222230">

                    <TextView
                        android:id="@+id/tvPolicyTitle"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:text="Policy Title"
                        android:textColor="@android:color/white"
                        android:textSize="20sp"
                        android:textStyle="bold"
                        android:layout_toStartOf="@id/btnClosePolicy"
                        android:layout_centerVertical="true"
                        android:layout_marginEnd="16dp"/>

                    <ImageView
                        android:id="@+id/btnClosePolicy"
                        android:layout_width="32dp"
                        android:layout_height="32dp"
                        android:src="@android:drawable/ic_menu_close_clear_cancel"
                        app:tint="#730692"
                        android:background="?attr/selectableItemBackgroundBorderless"
                        android:layout_alignParentEnd="true"
                        android:layout_centerVertical="true"/>
                </RelativeLayout>

                <View android:layout_width="match_parent" android:layout_height="2dp" android:background="#730692"/>

                <ScrollView
                    android:layout_width="match_parent"
                    android:layout_height="match_parent"
                    android:padding="20dp">

                    <TextView
                        android:id="@+id/tvPolicyContent"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:textColor="#CCCCCC"
                        android:textSize="15sp"
                        android:lineSpacingExtra="6dp"
                        android:text="Policy Content here..."/>
                </ScrollView>
            </LinearLayout>
        </androidx.cardview.widget.CardView>
    </FrameLayout>
"""

# Insert Overlay Layout right before </androidx.constraintlayout.widget.ConstraintLayout>
fragment_content = fragment_content.replace(
    "</androidx.constraintlayout.widget.ConstraintLayout>",
    overlay_layout + "</androidx.constraintlayout.widget.ConstraintLayout>"
)

with open(fragment_file, 'w', encoding='utf-8') as f:
    f.write(fragment_content)
print("Updated fragment_more.xml")


# 2. Update MoreFragment.kt
fragment_kt_file = r"c:\Aboneliklerim\app\src\main\java\com\aboneliklerim\app\MoreFragment.kt"
with open(fragment_kt_file, 'r', encoding='utf-8') as f:
    kt_content = f.read()

# Remove Safe DPI logic
kt_dpi_pattern = re.compile(r"// --- SAFE DPI \(UI SCALE\) ---.*?// --- EYE PROTECTION ---", re.DOTALL)
kt_content = kt_dpi_pattern.sub("// --- EYE PROTECTION ---", kt_content)

# Add Legal logic inside onViewCreated
legal_logic = """
        // Setup legal policy overlay click events
        val overlay = view.findViewById<android.widget.FrameLayout>(R.id.layoutPolicyOverlay)
        val tvTitle = view.findViewById<TextView>(R.id.tvPolicyTitle)
        val tvContent = view.findViewById<TextView>(R.id.tvPolicyContent)
        val btnClose = view.findViewById<android.widget.ImageView>(R.id.btnClosePolicy)

        val showPolicy = { titleRes: Int, contentRes: Int ->
            tvTitle.text = getString(titleRes)
            tvContent.text = getString(contentRes)
            overlay.visibility = View.VISIBLE
        }

        view.findViewById<LinearLayout>(R.id.rowPrivacyPolicy)?.setOnClickListener {
            showPolicy(R.string.policy_privacy_title, R.string.policy_privacy_content)
        }
        view.findViewById<LinearLayout>(R.id.rowAccountDeletion)?.setOnClickListener {
            showPolicy(R.string.policy_deletion_title, R.string.policy_deletion_content)
        }
        view.findViewById<LinearLayout>(R.id.rowCopyright)?.setOnClickListener {
            showPolicy(R.string.policy_copyright_title, R.string.policy_copyright_content)
        }
        view.findViewById<LinearLayout>(R.id.rowAdPartners)?.setOnClickListener {
            showPolicy(R.string.policy_adpartners_title, R.string.policy_adpartners_content)
        }

        btnClose?.setOnClickListener {
            overlay.visibility = View.GONE
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (overlay != null && overlay.visibility == View.VISIBLE) {
                    overlay.visibility = View.GONE
                } else {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })
"""

# Insert Legal logic at the end of onViewCreated
kt_content = kt_content.replace(
    """        setupCurrencyConverter(view)
    }""",
    """        setupCurrencyConverter(view)
""" + legal_logic + """
    }"""
)

with open(fragment_kt_file, 'w', encoding='utf-8') as f:
    f.write(kt_content)
print("Updated MoreFragment.kt")


# 3. Update DisplayHelper.kt
display_helper_file = r"c:\Aboneliklerim\app\src\main\java\com\aboneliklerim\app\DisplayHelper.kt"
with open(display_helper_file, 'r', encoding='utf-8') as f:
    display_content = f.read()

# Remove DPI logic from adjustDisplaySettings
display_dpi_pattern = re.compile(r"// For DPI scaling.*?configuration\.densityDpi = \(metrics\.densityDpi \* densityScale\)\.toInt\(\)", re.DOTALL)
display_content = display_dpi_pattern.sub("", display_content)

with open(display_helper_file, 'w', encoding='utf-8') as f:
    f.write(display_content)
print("Updated DisplayHelper.kt")


# 4. Remove strings from all strings.xml
base_dir = r"c:\Aboneliklerim\app\src\main\res"
folders = [d for d in os.listdir(base_dir) if d.startswith("values")]

string_keys_to_remove = ["safe_dpi", "dpi_small", "dpi_normal", "dpi_large", "dpi_very_large"]

fixed_count = 0

for folder in folders:
    file_path = os.path.join(base_dir, folder, "strings.xml")
    if not os.path.exists(file_path):
        continue
        
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
        
    original_content = content
    for key in string_keys_to_remove:
        # Match <string name="key">...</string> and newline
        pattern = re.compile(r'\s*<string name="' + key + r'">.*?</string>')
        content = pattern.sub("", content)
        
    if content != original_content:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
        fixed_count += 1

print(f"Removed DPI strings from {fixed_count} strings.xml files")
