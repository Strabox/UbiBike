package pt.ulisboa.tecnico.cmu.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;

import pt.ulisboa.tecnico.cmu.controllers.util.JsonViews;
import pt.ulisboa.tecnico.cmu.domain.exceptions.TrajectoryAlreadyExistException;
import pt.ulisboa.tecnico.cmu.domain.exceptions.TrajectoryDoesntExistException;

@Entity
public class User implements Comparable<User>{
	
	@JsonView(JsonViews.LowDetailed.class)
	@Id
	private String username;
	
	@JsonIgnore
	@Column(nullable = false)
	private String password;
	
	@JsonView(JsonViews.LowDetailed.class)
	@Column(nullable = false)
	private long points;
	
	@JsonView(JsonViews.HighlyDetailed.class)
	@OneToOne(cascade = {CascadeType.ALL},mappedBy = "user")
	private Bike reservedBike; 
	
	@JsonView(JsonViews.MediumDetailed.class)
	@OneToMany(cascade = CascadeType.ALL,orphanRemoval = true)
	@OrderBy("date DESC")
	private List<Trajectory> trajectories = new ArrayList<>();
	
	
	public User(String username,String password) {
		setUsername(username);
		setPassword(password);
		setPoints(0);
		reservedBike = null;
	}
	
	protected User() { }	//Needed for JPA/Hibernate and JSON
	
	public String getUsername(){
		return username;
	}
	
	public void setUsername(String username) {
		this.username = username;
	}
	
	public String getPassword(){
		return password;
	}
	
	public void setPassword(String password){
		this.password = password;
	}
	
	public long getPoints(){
		return points;
	}
	
	public void setPoints(long points){
		this.points = points;
	}
	
	public void addPoints(long points){
		this.points += points;
	}
	
	public void setReservedBike(Bike bike){
		this.reservedBike = bike;
	}
	
	public Bike getReservedBike(){
		return this.reservedBike;
	}
	
	public Collection<Trajectory> getTrajectories(){
		return this.trajectories;
	}
	
	public Trajectory getTrajectory(long date) throws TrajectoryDoesntExistException{
		for(Trajectory t : this.trajectories){
			if(t.getDate() == date){
				return t;
			}
		}
		throw new TrajectoryDoesntExistException();
	}
	
	public void addTrajectory(Trajectory t) throws TrajectoryAlreadyExistException{
		for(Trajectory traj : this.trajectories){
			if(t.equals(traj)){
				throw new TrajectoryAlreadyExistException();
			}
		}
		trajectories.add(t);
	}

	
	@Override
	public int compareTo(User o) {
		return username.compareTo(o.getUsername());
	}
	
}
