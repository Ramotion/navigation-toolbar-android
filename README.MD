## Basic usage

NavigationToolBarLayout is the successor of CoordinatorLayout. Therefore NavigationToolBarLayout
must be the root element of your layout. Displayed content must be inside
NavigationToolBarLayout, as shown below:

```xml
<com.ramotion.navigationtoolbar.NavigationToolBarLayout
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <include layout="@layout/content_layout"/>

    <android.support.design.widget.FloatingActionButdddton
        android:id="@+id/fab"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        app:layout_anchor="@id/com_ramotion_app_bar"
        app:layout_anchorGravity="bottom|end"
        app:srcCompat="@android:drawable/ic_dialog_email" />

</com.ramotion.navigationtoolbar.NavigationToolBarLayout>
```

Next, you must specify an adapter for NavigationToolBarLayout, from which,
NavigationToolBarLayout will receive the displayed View.

NavigationToolBarLayout contains `android.support.v7.widget.Toolbar` and
`android.support.design.widget.AppBarLayout`, access to which can be obtained through
the appropriate identifiers:
``` xml
@id/com_ramotion_toolbar <!-- identifier of Toolbar -->
@id/com_ramotion_app_bar <!-- identifier of AppBarLayout -->
```
Either through the appropriate properties of the NavigationToolBarLayout class:
```kotlin
val toolBar: Toolbar
val appBarLayout: AppBarLayout
```

Here are the attributes you can specify through XML or related setters:
* `headerOnScreenItemCount` - The maximum number of simultaneously displayed cards (items) in vertical orientation.
* `headerCollapsingBySelectDuration` - Collapsing animation duration of header (HeaderLayout), when you click on the card in vertical orientation.
* `headerTopBorderAtSystemBar` - Align the top card on the systembar or not.
* `headerVerticalItemWidth` - Specifies the width of the vertical card. It can be equal to `match_parent`, then the width of the card will be equal to the width of NavigationToolBarLayout.
* `headerVerticalGravity` - Specifies the alignment of the vertical card. Can take the value: left, center, right.