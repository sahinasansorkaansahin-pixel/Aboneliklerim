import re

with open('c:/Aboneliklerim/app/src/main/res/layout/fragment_more.xml', 'r', encoding='utf-8') as f:
    content = f.read()

google_signin_pattern = re.compile(r'(\s*<!-- Google Login Row -->\s*<LinearLayout android:id="@+id/rowGoogleSignIn".*?</LinearLayout>\s*<View android:id="@+id/dividerGoogle".*?/>)', re.DOTALL)
google_match = google_signin_pattern.search(content)
google_snippet = google_match.group(1) if google_match else ''
content = google_signin_pattern.sub('', content)

card_pattern = re.compile(r'(\s*<androidx\.cardview\.widget\.CardView\s*android:id="@+id/cardUserInfo".*?</androidx\.cardview\.widget\.CardView>)', re.DOTALL)
card_match = card_pattern.search(content)
card_snippet = card_match.group(1) if card_match else ''
content = card_pattern.sub('', content)

insertion_point = re.compile(r'(\s*<TextView\s*android:layout_width="wrap_content"\s*android:layout_height="wrap_content"\s*android:text="@string/appearance")')

account_card = f'''

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/account"
                android:textColor="@color/text_main"
                android:textSize="14sp"
                android:textStyle="bold"
                android:layout_marginStart="8dp"
                android:layout_marginBottom="8dp" />

            <androidx.cardview.widget.CardView
                android:id="@+id/cardGoogleWrapper"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginBottom="24dp"
                app:cardBackgroundColor="@color/surface_main"
                app:cardCornerRadius="20dp"
                app:cardElevation="0dp">

                <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
                    android:orientation="vertical" android:padding="8dp">{google_snippet}
                </LinearLayout>
            </androidx.cardview.widget.CardView>{card_snippet}
'''

new_content = insertion_point.sub(lambda m: account_card + m.group(1), content)

with open('c:/Aboneliklerim/app/src/main/res/layout/fragment_more.xml', 'w', encoding='utf-8') as f:
    f.write(new_content)

print('Success')
