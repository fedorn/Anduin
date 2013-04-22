package ru.ksu.niimm.cll.anduin

import com.twitter.scalding.{Tsv, TypedTsv, Job, Args}
import ru.ksu.niimm.cll.anduin.adjacency.AdjacencyListProcessor

/**
 * Given an adjacency list (see the output of [[AdjacencyListProcessor]]]),
 * this processor outputs the tensor entries (first - rows, then - columns) sorted by their slice indices (i.e., predicateId)
 *
 * @author Nikita Zhiltsov 
 */
class TensorHelperProcessor(args: Args) extends Job(args) {

  private val adjacencyList = TypedTsv[(String, String, String)](args("input")).read.rename((0, 1, 2) ->('predicateId, 'subject, 'object))

  private val entities =
    TypedTsv[(String, String)](args("inputEntityList")).read.rename((0, 1) ->('entityId, 'entityURI))

  val tensorEntries = adjacencyList.joinWithSmaller('subject -> 'entityURI, entities)
    .project(('predicateId, 'entityId, 'object)).rename('entityId -> 'subjectId)
    .joinWithSmaller('object -> 'entityURI, entities).project(('predicateId, 'subjectId, 'entityId)).rename('entityId -> 'objectId)
    .unique(('predicateId, 'subjectId, 'objectId))

  tensorEntries
    .filter(('predicateId, 'subjectId, 'objectId)) {
    // todo: a workaround, don't realize why there are empty entries after inner join
    fields: (String, String, String) =>
      fields._1 != null && fields._2 != null && fields._3 != null &&
        !fields._1.trim.isEmpty && !fields._2.trim.isEmpty && !fields._3.trim.isEmpty
  }
    .groupAll {
    _.sortBy('predicateId)
  }.write(Tsv(args("output")))

}
