package org.mastodon.detection;

import org.mastodon.HasErrorMessage;
import org.scijava.Cancelable;

import bdv.spimdata.SpimDataMinimal;
import net.imagej.ops.special.inplace.BinaryInplace1OnlyOp;

public interface DetectorOp extends BinaryInplace1OnlyOp< DetectionCreator, SpimDataMinimal >, Cancelable, HasErrorMessage
{}
