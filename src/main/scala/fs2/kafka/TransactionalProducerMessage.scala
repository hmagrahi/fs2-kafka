/*
 * Copyright 2018-2019 OVO Energy Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fs2.kafka

import cats.{FlatMap, Monad, Traverse}
import cats.syntax.applicative._

/**
  * [[TransactionalProducerMessage]] represents zero or more
  * [[CommittableProducerRecords]], together with an arbitrary passthrough
  * value, all of which can be used with [[TransactionalKafkaProducer]]
  * to produce messages and commit offsets within a single transaction.
  * [[TransactionalProducerMessage]]s can be created using one of the
  * following options.<br>
  * <br>
  * - `TransactionalProducerMessage#apply` to produce zero or more records,
  * commit the paired offsets, then emit a [[ProducerResult]] with the
  * results and specified passthrough value.<br>
  * - `TransactionalProducerMessage#one` to produce zero or more records,
  * commit exactly one offset, then emit a [[ProducerResult]] with the
  * results and specified passthrough value.<br>
  * <br>
  * The [[passthrough]] and [[records]] can be retrieved from an existing
  * [[TransactionalProducerMessage]] instance.<br>
  * <br>
  * For a [[TransactionalProducerMessage]] to be usable by [[TransactionalKafkaProducer]],
  * it needs a `Traverse[G]` and `FlatMap[G]` instance. These requirements
  * are captured in [[TransactionalProducerMessage]] as [[traverse]] and [[flatMap]].
  */
sealed abstract class TransactionalProducerMessage[F[_], G[+ _], +K, +V, +P] {

  /** The records to produce and commit. Can be empty for passthrough-only. */
  def records: G[CommittableProducerRecords[F, G, K, V]]

  /** The passthrough to emit once all [[records]] have been produced and committed. */
  def passthrough: P

  /** The flatMap instance for `G[_]`. Required by [[TransactionalKafkaProducer]]. */
  def flatMap: FlatMap[G]

  /** The traverse instance for `G[_]`. Required by [[TransactionalKafkaProducer]]. */
  def traverse: Traverse[G]
}

object TransactionalProducerMessage {
  private[this] final class TransactionalProducerMessageImpl[F[_], G[+ _], +K, +V, +P](
    override val records: G[CommittableProducerRecords[F, G, K, V]],
    override val passthrough: P,
    override val flatMap: FlatMap[G],
    override val traverse: Traverse[G]
  ) extends TransactionalProducerMessage[F, G, K, V, P] {
    override def toString: String =
      s"TransactionalProducerMessage($records, $passthrough)"
  }

  /**
    * Creates a new [[TransactionalProducerMessage]] for producing
    * zero or more [[CommittableProducerRecords]], then emitting a
    * [[ProducerResult]] with the results and `Unit` passthrough value.
    */
  def apply[F[_], G[+ _], K, V](
    records: G[CommittableProducerRecords[F, G, K, V]]
  )(
    implicit flatMap: FlatMap[G],
    traverse: Traverse[G]
  ): TransactionalProducerMessage[F, G, K, V, Unit] =
    apply(records, ())

  /**
    * Creates a new [[TransactionalProducerMessage]] for producing
    * zero or more [[CommittableProducerRecords]], then emitting a
    * [[ProducerResult]] with the results and specified passthrough value.
    */
  def apply[F[_], G[+ _], K, V, P](
    records: G[CommittableProducerRecords[F, G, K, V]],
    passthrough: P
  )(
    implicit flatMap: FlatMap[G],
    traverse: Traverse[G]
  ): TransactionalProducerMessage[F, G, K, V, P] =
    new TransactionalProducerMessageImpl(records, passthrough, flatMap, traverse)

  /**
    * Creates a new [[TransactionalProducerMessage]] for producing exactly
    * one [[CommittableProducerRecords]], then emitting a [[ProducerResult]]
    * with the result and `Unit` passthrough value.
    */
  def one[F[_], G[+ _], K, V, P](
    record: CommittableProducerRecords[F, G, K, V]
  )(
    implicit monad: Monad[G],
    traverse: Traverse[G]
  ): TransactionalProducerMessage[F, G, K, V, Unit] =
    one[F, G, K, V, Unit](record, ())

  /**
    * Creates a new [[TransactionalProducerMessage]] for producing exactly
    * one [[CommittableProducerRecords]], then emitting a [[ProducerResult]]
    * with the result and specified passthrough value.
    */
  def one[F[_], G[+ _], K, V, P](
    record: CommittableProducerRecords[F, G, K, V],
    passthrough: P
  )(
    implicit monad: Monad[G],
    traverse: Traverse[G]
  ): TransactionalProducerMessage[F, G, K, V, P] =
    apply[F, G, K, V, P](record.pure[G], passthrough)
}
