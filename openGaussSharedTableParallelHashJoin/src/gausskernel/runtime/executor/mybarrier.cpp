#include "executor/mybarrier.h"
#include "utils/palloc.h"

void myBarrierInit(myBarrier *barrier,int participants){
    barrier->phase=0;
    barrier->participants=participants;
    barrier->arrived=0;
    barrier->elected=0;
	barrier->condition_variable=(pthread_cond_t*)palloc(sizeof(pthread_cond_t));
	pthread_cond_init(barrier->condition_variable,NULL);
	barrier->mutex=(pthread_mutex_t*)palloc(sizeof(pthread_mutex_t));
	pthread_mutex_init(barrier->mutex,NULL);
}

int	myBarrierPhase(myBarrier *barrier){
    return barrier->phase;
}

bool myBarrierArriveAndWait(myBarrier *barrier){
    bool		release = false;
	bool		elected;
	int			start_phase;
	int			next_phase;

    pthread_mutex_lock(barrier->mutex);
	start_phase = barrier->phase;
	next_phase = start_phase + 1;
	++barrier->arrived;
	//printf("before: barrier arrived: %d participants: %d phase: %d\n", barrier->arrived, barrier->participants, barrier->phase);
	if (barrier->arrived == barrier->participants)
	{
		release = true;
		barrier->arrived = 0;
		barrier->phase = next_phase;
		barrier->elected = next_phase;
	}
	//printf("after: barrier arrived: %d participants: %d phase: %d release: %d\n", barrier->arrived, barrier->participants, barrier->phase, release);
    pthread_mutex_unlock(barrier->mutex);

	/*
	 * If we were the last expected participant to arrive, we can release our
	 * peers and return true to indicate that this backend has been elected to
	 * perform any serial work.
	 */
	if (release)
	{
		pthread_cond_broadcast(barrier->condition_variable);
        
		return true;
	}

	/*
	 * Otherwise we have to wait for the last participant to arrive and
	 * advance the phase.
	 */
	elected = false;
	for (;;)
	{
		/*
		 * We know that phase must either be start_phase, indicating that we
		 * need to keep waiting, or next_phase, indicating that the last
		 * participant that we were waiting for has either arrived or detached
		 * so that the next phase has begun.  The phase cannot advance any
		 * further than that without this backend's participation, because
		 * this backend is attached.
		 */
        pthread_mutex_lock(barrier->mutex);
		Assert(barrier->phase == start_phase || barrier->phase == next_phase);
		release = barrier->phase == next_phase;
		if (release && barrier->elected != next_phase)
		{
			/*
			 * Usually the backend that arrives last and releases the other
			 * backends is elected to return true (see above), so that it can
			 * begin processing serial work while it has a CPU timeslice.
			 * However, if the barrier advanced because someone detached, then
			 * one of the backends that is awoken will need to be elected.
			 */
			barrier->elected = barrier->phase;
			//elected = true;
		}
        //pthread_mutex_unlock(barrier->mutex);
		if (release){
			pthread_mutex_unlock(barrier->mutex);
			break;
		} else {
			pthread_cond_wait(barrier->condition_variable,barrier->mutex);
			pthread_mutex_unlock(barrier->mutex);
		}
		// pthread_mutex_lock(barrier->mutex);
		// pthread_cond_wait(barrier->condition_variable,barrier->mutex);
		// pthread_mutex_unlock(barrier->mutex);
	}
    //pthread_cond_broadcast(barrier->condition_variable);

	return elected;
}

int	myBarrierAttach(myBarrier *barrier){
    int			phase;

    pthread_mutex_lock(barrier->mutex);
	++barrier->participants;
	phase = barrier->phase;
    pthread_mutex_unlock(barrier->mutex);

	return phase;
}

bool myBarrierDetach(myBarrier *barrier, bool arrive){
    bool		release;
	bool		last;

    pthread_mutex_lock(barrier->mutex);
	Assert(barrier->participants > 0);
	--barrier->participants;

	/*
	 * If any other participants are waiting and we were the last participant
	 * waited for, release them.  If no other participants are waiting, but
	 * this is a BarrierArriveAndDetach() call, then advance the phase too.
	 */
	if ((arrive || barrier->participants > 0) &&
		barrier->arrived == barrier->participants)
	{
		release = true;
		barrier->arrived = 0;
		++barrier->phase;
	}
	else
		release = false;

	last = barrier->participants == 0;
    pthread_mutex_unlock(barrier->mutex);

	if (release)
        pthread_cond_broadcast(barrier->condition_variable);

	return last;
}