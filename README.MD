## Basic usage

NavigationToolBarLayout является наследником CoordinatorLayout. Поэтому NavigationToolBarLayout
должен быть корневым элементом вашего лэйаута. Отображаемый контент, должен находится внутри
NavigationToolBarLayout, как показано ниже:

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

Далее необходимо указать адаптер для NavigationToolBarLayout, из которого,
NavigationToolBarLayout будет получать отображаемые View.

NavigationToolBarLayout содержит в себе `android.support.v7.widget.Toolbar` и
`android.support.design.widget.AppBarLayout`, доступ к которым можно получить через
соотвествующие идентификаторы:
``` xml
@id/com_ramotion_toolbar <!-- идентификатор Toolbar -->
@id/com_ramotion_app_bar <!-- идентификатор AppBarLayout -->
```
Либо через соотвествующие свойства класса NavigationToolBarLayout:
```kotlin
val toolBar: Toolbar
val appBarLayout: AppBarLayout
```

Here are the attributes you can specify through XML or related setters:
* `headerOnScreenItemCount` - максимально количество одновременно показываемых карточек в вертикальном положении
* `headerCollapsingBySelectDuration` - время анимации сворачивания заголовка, при нажатии на карточку
* `headerTopBorderAtSystemBar` - вырвнивать верхнюю карточку по систембару или нет
* `headerVerticalItemWidth` - задает ширину вертикально карточки. Может быть равен `match_parent`, тогда ширина карточки будет равно ширине NavigationToolBarLayout
* `headerVerticalGravity` - задает вырванивание карточек в вертикальном положение. Может принимать значение: left, center, right