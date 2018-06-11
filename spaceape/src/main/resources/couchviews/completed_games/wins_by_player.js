function(doc) {
  if (doc.kind === 'completed_game' && doc.doc.winner.length !== 0) {
    emit(doc.doc.winner[0]);
  }
}
