package net.consensys.liszt.provemanager;

import net.consensys.liszt.core.common.Batch;

public interface ProveService {
  /** @param batch to prove starts prover for a givent batch */
  void proveBatch(Batch batch);
}
