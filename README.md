<a href="https://www.ramotion.com/agency/app-development/?utm_source=gthb&utm_medium=repo&utm_campaign=navigation-toolbar-android"><img src="https://github.com/Ramotion/folding-cell/blob/master/header.png"></a>

<a href="https://github.com/Ramotion/navigation-toolbar-android">
<img align="left" src="https://github.com/Ramotion/navigation-toolbar-android/blob/master/Navigation-toolbar.gif" width="480" height="360" /></a>

<p><h1 align="left">NAVIGATION TOOLBAR</h1></p>

<h4>Navigation toolbar is a Kotlin slide-modeled UI navigation controller.</h4>


___


<p><h6>We specialize in the designing and coding of custom UI for Mobile Apps and Websites.</h6>
<a href="https://www.ramotion.com/agency/app-development/?utm_source=gthb&utm_medium=repo&utm_campaign=navigation-toolbar-android">
<img src="https://github.com/ramotion/gliding-collection/raw/master/contact_our_team@2x.png" width="187" height="34"></a>
</p>
<p><h6>Stay tuned for the latest updates:</h6>
<a href="https://goo.gl/rPFpid" >
<img src="https://i.imgur.com/ziSqeSo.png/" width="156" height="28"></a></p>

Inspired by [Aurélien Salomon](https://dribbble.com/aureliensalomon) [shot](https://dribbble.com/shots/2940231-Google-Newsstand-Navigation-Pattern)

</br>

[![Twitter](https://img.shields.io/badge/Twitter-@Ramotion-blue.svg?style=flat)](http://twitter.com/Ramotion)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/92bd2e49f7e543cd8748c670b9e52ca7)](https://app.codacy.com/app/dvg4000/navigation-toolbar-android/dashboard)
[![Donate](https://img.shields.io/badge/Donate-PayPal-blue.svg)](https://paypal.me/Ramotion)

## Requirements

- Android 5.0 Lollipop (API lvl 21) or greater
- Your favorite IDE

## Installation
​
Just download the package from [here](http://central.maven.org/maven2/com/ramotion/navigationtoolbar/navigation-toolbar/0.1.3/navigation-toolbar-0.1.3.aar) and add it to your project classpath, or just use the maven repo:

Gradle:
```groovy
implementation 'com.ramotion.navigationtoolbar:navigation-toolbar:0.1.3'
```
SBT:
```scala
libraryDependencies += "com.ramotion.navigationtoolbar" % "navitagiton-toolbar" % "0.1.3"
```
Maven:
```xml
<dependency>
  <groupId>com.ramotion.navigationtoolbar</groupId>
  <artifactId>navigation-toolbar</artifactId>
  <version>0.1.3</version>
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

## 🗂 Check this library on other language:
<a href="https://github.com/Ramotion/navigation-toolbar"> 
<img src="https://github.com/ramotion/navigation-stack/raw/master/Swift@2x.png" width="178" height="81"></a>


## 📄 License

Navigation Toolbar Android is released under the MIT license.
See [LICENSE](./LICENSE) for details.

This library is a part of a <a href="https://github.com/Ramotion/android-ui-animation-components-and-libraries"><b>selection of our best UI open-source projects</b></a>

If you use the open-source library in your project, please make sure to credit and backlink to www.ramotion.com

## 📱 Get the Showroom App for Android to give it a try
Try this UI component and more like this in our Android app. Contact us if interested.

<a href="https://play.google.com/store/apps/details?id=com.ramotion.showroom" >
<img src="https://raw.githubusercontent.com/Ramotion/react-native-circle-menu/master/google_play@2x.png" width="104" height="34"></a>

<a href="https://www.ramotion.com/agency/app-development/?utm_source=gthb&utm_medium=repo&utm_campaign=navigation-toolbar-android">
<img src="https://github.com/ramotion/gliding-collection/raw/master/contact_our_team@2x.png" width="187" height="34"></a>

