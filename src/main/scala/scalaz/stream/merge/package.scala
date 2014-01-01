package scalaz.stream

import scalaz.concurrent.{Strategy, Task}
import scalaz.stream.Process._


package object merge {


  /**
   * Syntax helper, see mergeN below
   */
  def mergeN[A](p1: Process[Task, A], pn: Process[Task, A]*)
    (implicit S: Strategy = Strategy.DefaultStrategy): Process[Task, A] =
    mergeN(emitSeq(p1 +: pn))


  /**
   * Merges non-deterministically processes that are output of the `source` process.
   *
   * Merging stops when all processes generated by source have stopped, and all source process stopped as well.
   * Merging will also stop when resulting process terminated. In that case the cleanup of all `source`
   * processes is run, followed by cleanup of resulting process.
   *
   * When one of the source processes fails the mergeN process will fail with that reason.
   *
   * Merging is non-deterministic, but is fair in sense that every process is consulted, once it has `A` ready.
   * That means processes that are `faster` provide it's `A` more often than slower processes.
   *
   * Internally mergeN keeps small buffer that reads ahead up to `n` values of `A` where `n` equals to number
   * of active source streams. That does not mean that every `source` process is consulted in this read-ahead
   * cache, it just tries to be as much fair as possible when processes provide their `A` on almost the same speed.
   *
   *
   *
   */
  def mergeN[A](source: Process[Task, Process[Task, A]])
    (implicit S: Strategy = Strategy.DefaultStrategy): Process[Task, A] = {

    await(Task.delay(MergeX(MergeXStrategies.mergeN[A],source)(S)))({
      case mergeX => mergeX.downstreamO onComplete eval_(mergeX.downstreamClose(End))
    })


  }


}
