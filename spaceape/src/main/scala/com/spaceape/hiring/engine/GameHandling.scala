package com.spaceape.hiring.engine

/**
 * Unify aspects of game logic to provide a single API
 */
trait GameHandling extends MoveHandling with GameStateCalculation
