![header](./header.png)
<img src="https://github.com/Ramotion/navigation-toolbar-android/blob/master/Navigation-toolbar.gif" width="600" height="450" />
<br><br/>

# Navigation Toolbar for Android
[![Twitter](https://img.shields.io/badge/Twitter-@Ramotion-blue.svg?style=flat)](http://twitter.com/Ramotion)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/92bd2e49f7e543cd8748c670b9e52ca7)](https://app.codacy.com/app/dvg4000/navigation-toolbar-android/dashboard)
[![Donate](https://img.shields.io/badge/Donate-PayPal-blue.svg)](https://paypal.me/Ramotion)

Inspired by [Aurélien Salomon](https://dribbble.com/aureliensalomon) [shot](https://dribbble.com/shots/2940231-Google-Newsstand-Navigation-Pattern)

**Looking for developers for your project?**<br>
This project is maintained by Ramotion, Inc. We specialize in the designing and coding of custom UI for Mobile Apps and Websites.

<a href="https://dev.ramotion.com/?utm_source=gthb&utm_medium=special&utm_campaign=navigation-toolbar-contact-us">
<img src="https://github.com/ramotion/gliding-collection/raw/master/contact_our_team@2x.png" width="187" height="34"></a> <br>

The [Android mockup](https://store.ramotion.com/product/samsung-galaxy-s7-edge-mockups) available [here](https://store.ramotion.com/product/samsung-galaxy-s7-edge-mockups).

## Requirements

- Android 5.0 Lollipop (API lvl 21) or greater
- Your favorite IDE

## Installation
​
Just download the package from [here](http://central.maven.org/maven2/com/ramotion/navigationtoolbar/navigation-toolbar/0.1.0/navigation-toolbar-0.1.0.aar) and add it to your project classpath, or just use the maven repo:

Gradle:
```groovy
implementation 'com.ramotion.navigationtoolbar:navigation-toolbar:0.1.0'
```
SBT:
```scala
libraryDependencies += "com.ramotion.navigationtoolbar" % "navitagiton-toolbar" % "0.1.0"
```
Maven:
```xml
<dependency>
  <groupId>com.ramotion.navigationtoolbar</groupId>
  <artifactId>navigation-toolbar</artifactId>
  <version>0.1.0</version>
  <type>aar</type>
</dependency>
```

## Basic usage

NavigationToolBarLayout is the successor to CoordinatorLayout. Therefore, NavigationToolBarLayout
must be the root element of your layout. Displayed content must be inside
NavigationToolBarLayout, as shown below:

```xml
<com.ramotion.navigationtoolbar.NavigationToolBarLayout
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <include layout="@layout/content_layout"/>

    <android.support.design.widget.FloatingActionButton
        android:id="@+id/fab"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        app:layout_anchor="@id/com_ramotion_app_bar"
        app:layout_anchorGravity="bottom|end"
        app:srcCompat="@android:drawable/ic_dialog_email" />

</com.ramotion.navigationtoolbar.NavigationToolBarLayout>
```

Next, you must specify an adapter for NavigationToolBarLayout, from which
NavigationToolBarLayout will receive the displayed View.

NavigationToolBarLayout contains `android.support.v7.widget.Toolbar` and
`android.support.design.widget.AppBarLayout`, access to which can be obtained through
the appropriate identifiers:
``` xml
@id/com_ramotion_toolbar <!-- identifier of Toolbar -->
@id/com_ramotion_app_bar <!-- identifier of AppBarLayout -->
```
or through the appropriate properties of the NavigationToolBarLayout class:
```kotlin
val toolBar: Toolbar
val appBarLayout: AppBarLayout
```

Here are the attributes you can specify through XML or related setters:
* `headerOnScreenItemCount` - The maximum number of simultaneously displayed cards (items) in vertical orientation.
* `headerCollapsingBySelectDuration` - Collapsing animation duration of header (HeaderLayout), when you click on the card in vertical orientation.
* `headerTopBorderAtSystemBar` - Align the top card on the systembar or not.
* `headerVerticalItemWidth` - Specifies the width of the vertical card. It can be equal to `match_parent`, then the width of the card will be equal to the width of NavigationToolBarLayout.
* `headerVerticalGravity` - Specifies the alignment of the vertical card. Can take the values: left, center, or right.

## License
​
Navigation Toolbar for Android is released under the MIT license.
See [LICENSE](./LICENSE) for details.

# Get the Showroom App for Android to give it a try
Try our UI components in our mobile app. Contact us if interested.

<a href="https://play.google.com/store/apps/details?id=com.ramotion.showroom" >
<img src="https://raw.githubusercontent.com/Ramotion/react-native-circle-menu/master/google_play@2x.png" width="104" height="34"></a>
<a href="https://dev.ramotion.com/?utm_source=gthb&utm_medium=special&utm_campaign=navigation-toolbar-android-contact-us">
<img src="https://github.com/ramotion/gliding-collection/raw/master/contact_our_team@2x.png" width="187" height="34"></a>
<br>
<br>

Follow us for the latest updates:<br>
<a href="https://goo.gl/rPFpid" >
<img src="https://i.imgur.com/ziSqeSo.png/" width="156" height="28"></a>

