:boom: **IMPORTANT:** As of March 7, 2014, package `undofx` has been renamed to `org.fxmisc.undo` in the HEAD of the master branch. All upcoming builds will reflect this change. Please, update your source code.

UndoFX
======

UndoFX is a general-purpose undo manager for JavaFX (or Java applications in general).


Highlights
----------

**Arbitrary type of change objects.** Change objects don't have to implement any special interface, such as `UndoableEdit` in Swing.

**No requirements on the API of the control.** To add undo support for a control, you don't need the control to have a special API, such as `addUndoableEditListener(UndoableEditListener)` in Swing. This means you can add undo support to components that were not designed with undo support in mind, as long as you are able to observe changes on the component and the component provides API (of any sort) to reverse and reapply the effects of the changes.

**Immutable change objects** are encouraged. In contrast, `UndoableEdit` from Swing is mutable by design.

**Suppports merging of subsequent changes.**


API
---

```java
public interface UndoManager {
    boolean undo();
    boolean redo();

    ObservableBooleanValue undoAvailableProperty();
    boolean isUndoAvailable();

    ObservableBooleanValue redoAvailableProperty();
    boolean isRedoAvailable();

    void preventMerge();

    void close();
}
```

`undo()` undoes the most recent change, if there is any change to undo. Returns `true` if a change was undone, `false` otherwise.

`redo()` reapplies a previously undone change, if there is any change to reapply. Returns `true` if a change was reapplied, `false` otherwise.

`undoAvailable` and `redoAvailable` properties indicate whether there is a change to be undone or redone, respectively.

`preventMerge()` explicitly prevents the next (upcoming) change from being merged with the latest one.

`close()` stops observing change events and should be called when the UndoManager is not used anymore to prevent leaks.


Getting an `UndoManager` instance
---------------------------------

To get an instance of `UndoManager` you need:
 * a **stream of change events**;
 * a function to **apply** (redo) a change;
 * a function to **unapply** (undo) a change; and
 * optionally, a function to optionally **merge** two subsequent changes into a single change.

The _stream of change events_ is a [ReactFX](https://github.com/TomasMikula/ReactFX) `EventStream`. For an example of how you can construct one, have a look at the source code of the [demo below](#demo).

The _apply_, _unapply_ and _merge_ functions are all instances of [functional interfaces](http://download.java.net/jdk8/docs/api/java/util/function/package-summary.html) from JDK8, and thus can be instantiated using lambda expressions.

Once you have all these, you can use one of the factory methods from [UndoManagerFactory](https://googledrive.com/host/0B4a5AnNnZhkbVDRiZmxiMW1OYk0/javadoc/org/fxmisc/undo/UndoManagerFactory.html) to get an instance.

```java
EventStream<MyChange> changes = ...;
UndoManager undoManager = UndoManagerFactory.unlimitedHistoryUndoManager(
        changes,
        change -> applyChange(change),
        change -> unapplyChange(change),
        (c1, c2) -> mergeChanges(c1, c2));
```


Demo
----

This demo allow the user to change the color, radius and position of a circle, and subsequently undo and redo the performed changes.

![Screenshot of the CircleProperties demo](https://googledrive.com/host/0B4a5AnNnZhkbVDRiZmxiMW1OYk0/screenshots/circle-properties.png)

### Run from the pre-built JAR

[Download](https://googledrive.com/host/0B4a5AnNnZhkbVDRiZmxiMW1OYk0/downloads/) the pre-built "fat" JAR file and run

    java -cp undofx-demos-fat-yyyymmdd.jar org.fxmisc.undo.demo.CircleProperties

### Run from the source repo

    gradle CircleProperties

### Source code

[CircleProperties.java](https://github.com/TomasMikula/UndoFX/blob/master/undofx-demos/src/main/java/org/fxmisc/undo/demo/CircleProperties.java#L126-L146). See the highlighted lines for the gist of how the undo functionality is set up.


Requirements
------------

[JDK8](https://jdk8.java.net/download.html)

[ReactFX](https://github.com/TomasMikula/ReactFX). You can either place the ReactFX JAR on the classpath, or download the UndoFX _fat_ JAR that has ReactFX included.


License
-------

[BSD 2-Clause License](http://opensource.org/licenses/BSD-2-Clause)


Links
-----

[Download](https://googledrive.com/host/0B4a5AnNnZhkbVDRiZmxiMW1OYk0/downloads/)  
[Javadoc](https://googledrive.com/host/0B4a5AnNnZhkbVDRiZmxiMW1OYk0/javadoc/org/fxmisc/undo/package-summary.html)  
