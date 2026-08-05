from django.db import models


class User(models.Model):
    """Maps to openGauss B-mode table `user` under schema mysql_test_db."""

    id = models.AutoField(primary_key=True)
    name = models.CharField(max_length=50)
    age = models.IntegerField(null=True, blank=True)

    class Meta:
        db_table = "user"
        managed = False  # table prepared by SQL / docs guide

    def __str__(self):
        return f"User{{id={self.id}, name='{self.name}', age={self.age}}}"
