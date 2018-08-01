package uk.ac.ebi.biosamples.migration;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class AccessionQueueBothCallable implements Callable<Void> {
	
	private final Queue<String> oldQueue;
	private final Set<String> oldSet = new HashSet<>();
	private final AtomicBoolean oldFlag;
	private final Queue<String> newQueue;
	private final Set<String> newSet = new HashSet<>();
	private final AtomicBoolean newFlag;
	private final Queue<String> bothQueue;
	private final AtomicBoolean bothFlag;

	private final Logger log = LoggerFactory.getLogger(getClass());
	
	public AccessionQueueBothCallable(Queue<String> oldQueue, AtomicBoolean oldFlag, 
			Queue<String> newQueue, AtomicBoolean newFlag,
			Queue<String> bothQueue, AtomicBoolean bothFlag) {
		this.oldQueue = oldQueue;
		this.oldFlag = oldFlag;
		this.newQueue = newQueue;
		this.newFlag = newFlag;
		this.bothQueue = bothQueue;
		this.bothFlag = bothFlag;
	}
	
	
	
	@Override
	public Void call() throws Exception {
		log.info("Started AccessionQueueBothCallable.call()");		
		Set<String> toIgnore = new HashSet<>();
		log.info("read accessions to ignore");

		while (!oldFlag.get() || !oldQueue.isEmpty() || !newFlag.get() || !newQueue.isEmpty()) {
			if (!oldFlag.get() || !oldQueue.isEmpty()) {
				String next = oldQueue.poll();
				if (next != null && !toIgnore.contains(next)) {
					oldSet.add(next);
					if (newSet.contains(next)) {
						while (!bothQueue.offer(next)) {
							Thread.sleep(100);
						}
					}
				}
			}
			if (!newFlag.get() || !newQueue.isEmpty()) {
				String next = newQueue.poll();
				if (next != null && !toIgnore.contains(next)) {
					newSet.add(next);
					if (oldSet.contains(next)) {
						while (!bothQueue.offer(next)) {
							Thread.sleep(100);
						}
					}
				}
			}
		}
		
		//at his point we should be able to generate the differences in the sets
		
		Set<String> newOnly = Sets.difference(newSet, oldSet);
		Set<String> oldOnly = Sets.difference(oldSet, newSet);
		List<String> newOnlyList = Lists.newArrayList(newOnly);
		Collections.sort(newOnlyList);
		List<String> oldOnlyList = Lists.newArrayList(oldOnly);
		Collections.sort(oldOnlyList);
		log.info("Samples only in new "+newOnlyList.size());
		log.info("Samples only in old "+oldOnlyList.size());

		int i;
		Iterator<String> accIt;
		
		accIt = newOnlyList.iterator();
		i = 0;
		while (accIt.hasNext()) {
			log.warn("Sample only in new "+accIt.next());
			i++;
		}
		
		accIt = oldOnlyList.iterator();
		i = 0;
		while (accIt.hasNext()) {
			log.warn("Sample only in old "+accIt.next());
			i++;
		}
		 
		
		bothFlag.set(true);
		log.info("Finished AccessionQueueBothCallable.call(");
		
		return null;
	}
	
}
