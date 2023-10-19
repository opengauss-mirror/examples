#ifndef MYBARRIER_H
#define MYBARRIER_H

#include <pthread.h>
typedef struct myBarrier
{
    int			phase;			/* phase counter */
	int			participants;	/* the number of participants attached */
	int			arrived;		/* the number of participants that have
								 * arrived */
	int			elected;		/* highest phase elected */
	pthread_cond_t *condition_variable;
	pthread_mutex_t* mutex;
} myBarrier;

extern void myBarrierInit(myBarrier *barrier,int participants);
extern int	myBarrierPhase(myBarrier *barrier);
extern bool myBarrierArriveAndWait(myBarrier *barrier);
extern int	myBarrierAttach(myBarrier *barrier);
extern bool myBarrierDetach(myBarrier *barrier, bool arrive);

#endif