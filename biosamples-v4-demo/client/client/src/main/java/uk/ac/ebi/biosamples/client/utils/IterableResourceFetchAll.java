package uk.ac.ebi.biosamples.client.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.client.Hop;
import org.springframework.hateoas.client.Traverson;
import org.springframework.hateoas.client.Traverson.TraversalBuilder;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriTemplate;

import java.net.URI;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.IntFunction;

//import org.springframework.hateoas.UriTemplate;

public class IterableResourceFetchAll<T> implements Iterable<Resource<T>> {
	
	private Logger log = LoggerFactory.getLogger(getClass());	
	
	private final Traverson traverson;
	private final RestOperations restOperations;
	private final Hop[] hops;
	private final ParameterizedTypeReference<PagedResources<Resource<T>>> parameterizedTypeReference;
	private final MultiValueMap<String,String> params;
	private final ExecutorService executor;

	/**
	 * ParameterizedTypeReference must be ParameterizedTypeReference<PagedResources<Resource<T>>> but this
	 * information is lost due to type erasure of the generic on compilation, and therefore has to be
	 * passed manually.
	 * 
	 * @param traverson
	 * @param restOperations
	 * @param parameterizedTypeReference
	 * @param rels
	 */
	public IterableResourceFetchAll(ExecutorService executor, Traverson traverson, RestOperations restOperations, 
			ParameterizedTypeReference<PagedResources<Resource<T>>> parameterizedTypeReference,
			MultiValueMap<String,String> params, String... rels) {
		this(executor, traverson, restOperations, parameterizedTypeReference, params, 
				Arrays.stream(rels).map(rel -> Hop.rel(rel)).toArray(new IntFunction<Hop[]>(){
					@Override
					public Hop[] apply(int value) {
						return new Hop[value];
					}
				}));
	}

	/**
	 * ParameterizedTypeReference must be ParameterizedTypeReference<PagedResources<Resource<T>>> but this
	 * information is lost due to type erasure of the generic on compilation, and therefore has to be
	 * passed manually.
	 * 
	 * @param traverson
	 * @param restOperations
	 * @param parameterizedTypeReference
     * @param hops
	 */
	public IterableResourceFetchAll(ExecutorService executor, Traverson traverson, RestOperations restOperations, 
			ParameterizedTypeReference<PagedResources<Resource<T>>> parameterizedTypeReference,
			MultiValueMap<String,String> params, Hop... hops) {
		this.executor = executor;
		this.traverson = traverson;
		this.restOperations = restOperations;
		this.hops = hops;
		this.parameterizedTypeReference = parameterizedTypeReference;
		this.params = params;
	}
	
	public Iterator<Resource<T>> iterator() {
		
		TraversalBuilder traversonBuilder = null;
		for (Hop hop : hops) {
			if (traversonBuilder == null) {
				traversonBuilder = traverson.follow(hop);
			} else {
				traversonBuilder.follow(hop);
			}
		}
		
		//get the first page
		URI uri = UriComponentsBuilder.fromHttpUrl(traversonBuilder.asLink().getHref())
				.queryParams(params).build().toUri();
        RequestEntity<Void> requestEntity = RequestEntity.get(uri).accept(MediaTypes.HAL_JSON).build();
		ResponseEntity<PagedResources<Resource<T>>> responseEntity = restOperations.exchange(requestEntity,
				parameterizedTypeReference);
		return new IteratorResourceFetchAll<T>(responseEntity.getBody(), restOperations, parameterizedTypeReference, executor);
	}

	private static class IteratorResourceFetchAll<U> implements Iterator<Resource<U>> {
		
		private Logger log = LoggerFactory.getLogger(getClass());	
		
		private final RestOperations restOperations;
		private final ExecutorService executor;		
		private final ParameterizedTypeReference<PagedResources<Resource<U>>> parameterizedTypeReference;
		private PagedResources<Resource<U>> page;
		private Iterator<Resource<U>> pageIterator;
		private Future<PagedResources<Resource<U>>> nextPageFuture;
		
		public IteratorResourceFetchAll(PagedResources<Resource<U>> page, RestOperations restOperations, 
				ParameterizedTypeReference<PagedResources<Resource<U>>> parameterizedTypeReference,
				ExecutorService executor) {

			this.page = page;
			this.pageIterator = page.iterator();
			this.restOperations = restOperations;
			this.executor = executor;
			this.parameterizedTypeReference = parameterizedTypeReference;
		}
		
		@Override
		synchronized public boolean hasNext() {
			//pre-emptively grab the next page as a future
			if (nextPageFuture == null && page.hasLink(Link.REL_NEXT)) {
				
				Link nextLink = page.getLink(Link.REL_NEXT);
				URI uri;
				if (nextLink.isTemplated()) {
					UriTemplate uriTemplate = new UriTemplate(nextLink.getHref());
					uri = uriTemplate.expand();
				} else { 
					uri = URI.create(nextLink.getHref());					
				}
				log.trace("getting next page uri "+uri);
				
				nextPageFuture = executor.submit(new NextPageCallable<U>(restOperations, parameterizedTypeReference, uri));
			}
			
			if (pageIterator.hasNext()) {
				return true;
			}
			//at the end of this page, move to next
			if (nextPageFuture != null) {
				try {
					page = nextPageFuture.get();
					nextPageFuture = null;
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				} catch (ExecutionException e) {
					throw new RuntimeException(e);
				}
				pageIterator = page.iterator();
				return hasNext();
			} 
			return false;
		}
	
		@Override
		public Resource<U> next() {
			if (pageIterator.hasNext()) {
				return pageIterator.next();
			}

			//at the end of this page, move to next
			if (nextPageFuture != null) {
				try {
					page = nextPageFuture.get();
					pageIterator = page.iterator();
					nextPageFuture = null;
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				} catch (ExecutionException e) {
					throw new RuntimeException(e);
				}
				if (pageIterator.hasNext()) {
					return pageIterator.next();
				}
			} 
			//no more in this iterator and no more pages, so end	
			throw new NoSuchElementException();
		}
			
		
		private static class NextPageCallable<V> implements Callable<PagedResources<Resource<V>>> {
			
			private Logger log = LoggerFactory.getLogger(getClass());	

			private final RestOperations restOperations;
			private final URI uri;
			private final ParameterizedTypeReference<PagedResources<Resource<V>>> parameterizedTypeReference;
			
			public NextPageCallable(RestOperations restOperations, 
					ParameterizedTypeReference<PagedResources<Resource<V>>> parameterizedTypeReference,
					URI uri) {
				this.restOperations = restOperations;
				this.uri = uri;
				this.parameterizedTypeReference = parameterizedTypeReference;
			}
			
			@Override
			public PagedResources<Resource<V>> call() throws Exception {
				RequestEntity<Void> requestEntity = RequestEntity.get(uri).accept(MediaTypes.HAL_JSON).build();
				ResponseEntity<PagedResources<Resource<V>>> responseEntity = restOperations.exchange(requestEntity,
						parameterizedTypeReference);
				return responseEntity.getBody();				
			}
			
		}
			
	}
}