package com.spaceape.hiring.dao

class GameCompleter(
  override protected val activeDao: ActiveGameDao,
  override protected val completedDao: CompletedGameDao
) extends GameCompletion
