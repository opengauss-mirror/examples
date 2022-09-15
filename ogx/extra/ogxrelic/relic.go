package ogxrelic

import (
	"context"

	"github.com/newrelic/go-agent/v3/newrelic"
	"github.com/newrelic/go-agent/v3/newrelic/sqlparse"

	"gitee.com/chentanyang/ogx"
)

type QueryHook struct {
	baseSegment newrelic.DatastoreSegment
}

var _ ogx.QueryHook = (*QueryHook)(nil)

type nrOgxCtxKey string

const nrOgxSegmentKey nrOgxCtxKey = "nrogxsegment"

// NewQueryHook creates a new ogx.QueryHook which reports database usage
// information to new relic.
func NewQueryHook(options ...Option) *QueryHook {
	h := &QueryHook{}
	for _, o := range options {
		o(h)
	}
	return h
}

func (q *QueryHook) BeforeQuery(ctx context.Context, qe *ogx.QueryEvent) context.Context {
	segment := q.baseSegment

	if qe.Model != nil {
		if t, ok := qe.Model.(ogx.TableModel); ok {
			segment.Operation = qe.Operation()
			segment.Collection = t.Table().Name
		} else {
			sqlparse.ParseQuery(&segment, qe.Query)
		}
	} else {
		sqlparse.ParseQuery(&segment, qe.Query)
	}
	segment.StartTime = newrelic.FromContext(ctx).StartSegmentNow()
	return context.WithValue(ctx, nrOgxSegmentKey, &segment)

}
func (q *QueryHook) AfterQuery(ctx context.Context, qe *ogx.QueryEvent) {
	segment := ctx.Value(nrOgxSegmentKey).(*newrelic.DatastoreSegment)
	segment.End()
}
