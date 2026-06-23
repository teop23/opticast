package com.opticast.model

/**
 * Camera focus behaviour. A live camera property (not part of a streaming target).
 *
 * - [AUTO]: continuous autofocus; tap-to-focus does a one-shot lock on the tapped subject.
 * - [LOCKED]: freeze focus at its current distance so AF stops hunting (tripod/webcam framing).
 * - [INFINITY]: lock focus at infinity. For fixed mounts pointed at a far scene (e.g. a
 *   helmet/handlebar cam looking down the road) so a near, bright object — a dashboard — can't
 *   steal focus.
 */
enum class FocusMode { AUTO, LOCKED, INFINITY }
