UndoFX
======

UndoFX is a general-purpose undo manager for JavaFX (or Java applications in general).


Highlights
----------

**Arbitrary type of change objects.** Change objects don't have to implement any special interface, such as `UndoableEdit` in Swing.

**No requirements on the API of the control.** To add undo support for a control, you don't need the control to have a special API, such as `addUndoableEditListener(UndoableEditListener)` in Swing. This means you can add undo support to components that were not designed with undo support in mind, as long as you are able to observe changes on the component and the component provides API (of any sort) to reverse and reapply the effects of the changes.

**Immutable change objects** are encouraged. In contrast, `UndoableEdit` from Swing is mutable by design.

Suppports **merging of successive changes.**

Supports **marking** a state (position in the history) when the document was last saved.


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

    void forgetHistory();

    void mark();
    UndoPosition getCurrentPosition();

    ObservableBooleanValue atMarkedPositionProperty();
    boolean isAtMarkedPosition();

    void close();

    interface UndoPosition {
        void mark();
        boolean isValid();
    }
}
```

`undo()` undoes the most recent change, if there is any change to undo. Returns `true` if a change was undone, `false` otherwise.

`redo()` reapplies a previously undone change, if there is any change to reapply. Returns `true` if a change was reapplied, `false` otherwise.

`undoAvailable` and `redoAvailable` properties indicate whether there is a change to be undone or redone, respectively.

`preventMerge()` explicitly prevents the next (upcoming) change from being merged with the latest one.

`forgetHistory()` forgets all changes prior to the current position in the change history.

`mark()` sets a mark at the current position in the change history. This is meant to be used when the document is saved.

`getCurrentPosition()` returns a handle to the current position in the change history. This handle can be used to mark this position later, even if it is not the current position anymore.

`atMarkedPosition` property indicates whether the mark is set on the current position in the change history. This can be used to tell whether there are any unsaved changes.

`close()` stops observing change events and should be called when the UndoManager is not used anymore to prevent leaks.


Getting an `UndoManager` instance
---------------------------------

To get an instance of `UndoManager` you need:
 * a **stream of change events**;
 * a function to **invert** a change;
 * a function to **apply** a change; and
 * optionally, a function to optionally **merge** two subsequent changes into a single change.

The _stream of change events_ is a [ReactFX](http://www.reactfx.org/) `EventStream`. For an example of how you can construct one, have a look at the source code of the [demo below](#demo).

The _invert_, _apply_, and _merge_ functions are all instances of [functional interfaces](http://docs.oracle.com/javase/8/docs/api/java/util/function/package-summary.html) from JDK8, and thus can be instantiated using lambda expressions.

You also need to make sure that your change objects properly implement `equals`.

Once you have all these, you can use one of the factory methods from [UndoManagerFactory](http://www.fxmisc.org/undo/javadoc/org/fxmisc/undo/UndoManagerFactory.html) to get an instance.

```java
EventStream<MyChange> changes = ...;
UndoManager undoManager = UndoManagerFactory.unlimitedHistoryUndoManager(
        changes,
        change -> invertChange(change),
        change -> applyChange(change),
        (c1, c2) -> mergeChanges(c1, c2));
```


Demo
----

This demo lets the user change the color, radius and position of a circle, and subsequently undo and redo the performed changes.

Multiple changes of one property in a row are merged together, so, for example, multiple radius changes in a row are tracked as one change.

There is also a "Save" button that fakes a save operation. It is enabled only when changes have been made or undone since the last save.

![Screenshot of the CircleProperties demo](https://googledrive.com/host/0B4a5AnNnZhkbVDRiZmxiMW1OYk0/screenshots/circle-properties.png)

### Run from the pre-built JAR

[Download](https://github.com/TomasMikula/UndoFX/releases/download/v1.3.1/undofx-demos-fat-1.3.1.jar) the pre-built "fat" JAR file and run

    java -cp undofx-demos-fat-1.3.1.jar org.fxmisc.undo.demo.CircleProperties

### Run from the source repo

    gradle CircleProperties

### Source code

[CircleProperties.java](https://github.com/TomasMikula/UndoFX/blob/master/undofx-demos/src/main/java/org/fxmisc/undo/demo/CircleProperties.java#L180-L202). See the highlighted lines for the gist of how the undo functionality is set up.


Requirements
------------

[JDK8](https://jdk8.java.net/download.html)


Dependencies
------------

[ReactFX](https://github.com/TomasMikula/ReactFX). If you don't use Maven/Gradle/Sbt/Ivy to manage your dependencies, you will have to either place the ReactFX JAR on the classpath, or download the UndoFX _fat_ JAR (see below) that has ReactFX included.


Use UndoFX in your project
--------------------------

### Maven coordinates

| Group ID        | Artifact ID | Version |
| :-------------: | :---------: | :-----: |
| org.fxmisc.undo | undofx      | 1.3.1   |

### Gradle example

```groovy
dependencies {
    compile group: 'org.fxmisc.undo', name: 'undofx', version: '1.3.1'
}
```

### Sbt example

```scala
libraryDependencies += "org.fxmisc.undo" % "undofx" % "1.3.1"
```

### Manual download

Download [the JAR file](https://github.com/TomasMikula/UndoFX/releases/download/v1.3.1/undofx-1.3.1.jar) or [the fat JAR file (including dependencies)](https://github.com/TomasMikula/UndoFX/releases/download/v1.3.1/undofx-fat-1.3.1.jar) and place it on your classpath.


License
-------

[BSD 2-Clause License](http://opensource.org/licenses/BSD-2-Clause)


Links
-----

[API Documentation (Javadoc)](http://fxmisc.github.io/undo/javadoc/1.3.1/org/fxmisc/undo/package-summary.html)  
